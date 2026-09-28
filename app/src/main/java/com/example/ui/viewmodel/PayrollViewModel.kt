package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BonEntity
import com.example.data.model.CalendarDayEntity
import com.example.data.model.CalculationResult
import com.example.data.model.DayStatus
import com.example.data.model.FixedPostEntity
import com.example.data.model.FixedPostType
import com.example.data.model.HourlyWorkerEntity
import com.example.data.model.PaymentType
import com.example.data.model.QuinzaineEntity
import com.example.data.model.TransportEntity
import com.example.data.model.WorkerGroupEntity
import com.example.data.repository.PayrollRepository
import com.example.engine.PayrollCalculationEngine
import com.example.util.DateHelper
import com.example.util.PrevisionPdfGenerator
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PayrollViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PayrollRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = PayrollRepository(database)
    }

    val allQuinzaines: StateFlow<List<QuinzaineEntity>> = repository.allQuinzaines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeQuinzaineId = MutableStateFlow<Long?>(null)
    val activeQuinzaineId: StateFlow<Long?> = _activeQuinzaineId.asStateFlow()

    init {
        viewModelScope.launch {
            val initialId = repository.seedInitialDataIfEmpty()
            _activeQuinzaineId.value = initialId
        }
    }

    val activeQuinzaine: StateFlow<QuinzaineEntity?> = _activeQuinzaineId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getQuinzaine(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val workerGroups: StateFlow<List<WorkerGroupEntity>> = _activeQuinzaineId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getWorkerGroups(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hourlyWorkers: StateFlow<List<HourlyWorkerEntity>> = _activeQuinzaineId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getHourlyWorkers(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fixedPosts: StateFlow<List<FixedPostEntity>> = _activeQuinzaineId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getFixedPosts(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarDays: StateFlow<List<CalendarDayEntity>> = _activeQuinzaineId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getCalendarDays(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Section Indépendante: Prévision Transport (Isolé de la prévision de la quinzaine)
    val transports: StateFlow<List<TransportEntity>> = _activeQuinzaineId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getTransports(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Section Indépendante: Bons (Informations uniquement, sans prévision)
    val bons: StateFlow<List<BonEntity>> = _activeQuinzaineId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getBons(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Real-time automatic calculation flow combining all active state
    val calculationResult: StateFlow<CalculationResult> = combine(
        activeQuinzaine.filterNotNull(),
        workerGroups,
        hourlyWorkers,
        fixedPosts,
        calendarDays
    ) { quinzaine, groups, hourly, fixed, days ->
        PayrollCalculationEngine.calculate(
            quinzaine = quinzaine,
            workerGroups = groups,
            hourlyWorkers = hourly,
            fixedPosts = fixed,
            calendarDays = days
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CalculationResult(
            currentAmount = 0.0,
            totalNormalSalaries = 0.0,
            totalHolidaySalaries = 0.0,
            totalHourlySalaries = 0.0,
            totalFixedPosts = 0.0,
            totalRemainingAdditional = 0.0,
            finalForecast = 0.0,
            totalWorkers = 0,
            normalWorkDaysCount = 0,
            cancelledDaysCount = 0,
            holidayNormalDaysCount = 0,
            holidayDoubleDaysCount = 0,
            holidayCustomDaysCount = 0,
            unpaidDaysCount = 0,
            totalRemainingDaysCount = 0
        )
    )

    fun selectQuinzaine(id: Long) {
        _activeQuinzaineId.value = id
    }

    fun createNewQuinzaine(
        title: String,
        startDate: String,
        currentDate: String,
        endDate: String,
        currentAmount: Double,
        currency: String = "DH"
    ) {
        viewModelScope.launch {
            val quinzaine = QuinzaineEntity(
                title = title.ifBlank { "Nouvelle quinzaine" },
                startDate = startDate,
                currentDate = currentDate,
                endDate = endDate,
                currentAmount = currentAmount,
                currency = currency
            )
            val newId = repository.createQuinzaine(quinzaine)
            _activeQuinzaineId.value = newId
        }
    }

    fun updateCurrentAmount(amount: Double) {
        val current = activeQuinzaine.value ?: return
        viewModelScope.launch {
            repository.updateQuinzaine(current.copy(currentAmount = amount))
        }
    }

    fun updateQuinzaineDetails(
        title: String,
        startDate: String,
        currentDate: String,
        endDate: String,
        currentAmount: Double,
        currency: String
    ) {
        val current = activeQuinzaine.value ?: return
        viewModelScope.launch {
            repository.updateQuinzaine(
                current.copy(
                    title = title,
                    startDate = startDate,
                    currentDate = currentDate,
                    endDate = endDate,
                    currentAmount = currentAmount,
                    currency = currency
                )
            )
        }
    }

    fun toggleQuinzaineStatus() {
        val current = activeQuinzaine.value ?: return
        viewModelScope.launch {
            repository.updateQuinzaine(current.copy(isCompleted = !current.isCompleted))
        }
    }

    fun deleteCurrentQuinzaine(id: Long) {
        viewModelScope.launch {
            repository.deleteQuinzaine(id)
            val remaining = allQuinzaines.value.filter { it.id != id }
            if (remaining.isNotEmpty()) {
                _activeQuinzaineId.value = remaining.first().id
            } else {
                _activeQuinzaineId.value = null
            }
        }
    }

    fun duplicateCurrentQuinzaine(id: Long) {
        viewModelScope.launch {
            val current = allQuinzaines.value.find { it.id == id } ?: return@launch
            val newTitle = "${current.title} (Copie)"
            val newId = repository.duplicateQuinzaine(id, newTitle)
            if (newId > 0) {
                _activeQuinzaineId.value = newId
            }
        }
    }

    fun loadOfficialDemoScenario() {
        viewModelScope.launch {
            val newId = repository.loadDemoTestScenario()
            _activeQuinzaineId.value = newId
        }
    }

    // --- Worker Groups (Fully Customizable) ---
    fun addWorkerGroup(
        name: String,
        count: Int,
        paymentType: PaymentType = PaymentType.DAILY,
        dailyRate: Double = 0.0,
        hourlyRate: Double = 0.0,
        hoursWorked: Double = 0.0,
        customAmount: Double = 0.0,
        description: String = ""
    ) {
        val qId = _activeQuinzaineId.value ?: return
        viewModelScope.launch {
            repository.insertWorkerGroup(
                WorkerGroupEntity(
                    quinzaineId = qId,
                    name = name.ifBlank { "Groupe ${workerGroups.value.size + 1}" },
                    workerCount = count.coerceAtLeast(1),
                    paymentTypeString = paymentType.name,
                    dailyRate = dailyRate.coerceAtLeast(0.0),
                    hourlyRate = hourlyRate.coerceAtLeast(0.0),
                    hoursWorked = hoursWorked.coerceAtLeast(0.0),
                    customAmount = customAmount.coerceAtLeast(0.0),
                    description = description
                )
            )
        }
    }

    fun updateWorkerGroup(group: WorkerGroupEntity) {
        viewModelScope.launch {
            repository.updateWorkerGroup(group)
        }
    }

    fun deleteWorkerGroup(group: WorkerGroupEntity) {
        viewModelScope.launch {
            repository.deleteWorkerGroup(group)
        }
    }

    fun duplicateWorkerGroup(group: WorkerGroupEntity) {
        val qId = _activeQuinzaineId.value ?: return
        viewModelScope.launch {
            repository.insertWorkerGroup(
                group.copy(
                    id = 0,
                    quinzaineId = qId,
                    name = "${group.name} (Copie)"
                )
            )
        }
    }

    // --- Hourly Workers ---
    fun addHourlyWorker(name: String, hourlyRate: Double, hoursWorked: Double, targetAmount: Double? = null, description: String = "") {
        val qId = _activeQuinzaineId.value ?: return
        viewModelScope.launch {
            val effectiveHours = if (targetAmount != null && targetAmount > 0 && hourlyRate > 0) {
                targetAmount / hourlyRate
            } else {
                hoursWorked
            }
            repository.insertHourlyWorker(
                HourlyWorkerEntity(
                    quinzaineId = qId,
                    name = name.ifBlank { "Équipe horaire" },
                    hourlyRate = hourlyRate.coerceAtLeast(0.0),
                    hoursWorked = effectiveHours.coerceAtLeast(0.0),
                    targetAmount = targetAmount,
                    description = description
                )
            )
        }
    }

    fun updateHourlyWorker(worker: HourlyWorkerEntity) {
        viewModelScope.launch {
            repository.updateHourlyWorker(worker)
        }
    }

    fun deleteHourlyWorker(worker: HourlyWorkerEntity) {
        viewModelScope.launch {
            repository.deleteHourlyWorker(worker)
        }
    }

    // --- Fixed Posts (Fully Customizable) ---
    fun addFixedPost(
        name: String,
        category: String,
        type: FixedPostType,
        amount: Double,
        quantity: Int = 1,
        daysCount: Int = 1,
        isDirectLumpSum: Boolean = true
    ) {
        val qId = _activeQuinzaineId.value ?: return
        viewModelScope.launch {
            repository.insertFixedPost(
                FixedPostEntity(
                    quinzaineId = qId,
                    name = name.ifBlank { category.ifBlank { "Poste fixe" } },
                    category = category.ifBlank { "Général" },
                    typeString = type.name,
                    amount = amount.coerceAtLeast(0.0),
                    quantity = quantity.coerceAtLeast(1),
                    daysCount = daysCount.coerceAtLeast(1),
                    isDirectLumpSum = isDirectLumpSum
                )
            )
        }
    }

    fun updateFixedPost(post: FixedPostEntity) {
        viewModelScope.launch {
            repository.updateFixedPost(post)
        }
    }

    fun deleteFixedPost(post: FixedPostEntity) {
        viewModelScope.launch {
            repository.deleteFixedPost(post)
        }
    }

    // --- Calendar Day Status ---
    fun setDayStatus(
        date: String,
        status: DayStatus,
        note: String = "",
        holidayCustomAmountPerWorker: Double = 0.0
    ) {
        val qId = _activeQuinzaineId.value ?: return
        viewModelScope.launch {
            repository.setDayStatus(qId, date, status, note, holidayCustomAmountPerWorker)
        }
    }

    // --- Share Formatter ---
    fun buildShareSummaryText(): String {
        val q = activeQuinzaine.value ?: return ""
        val res = calculationResult.value

        return buildString {
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📋 PRÉVISION PAIE — ${q.title}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📅 Période: ${DateHelper.formatFrenchShort(q.startDate)} au ${DateHelper.formatFrenchShort(q.endDate)}")
            appendLine("⏱️ Date d'arrêté: ${DateHelper.formatFrenchShort(q.currentDate)}")
            appendLine("💼 Montant actuel: ${res.formattedCurrentAmount}")
            appendLine("👥 Effectif total: ${res.totalWorkers} travailleurs")
            appendLine("🗓️ Jours restants applicables: ${res.normalWorkDaysCount} jours")
            if (res.cancelledDaysCount > 0) {
                appendLine("🔴 Jours annulés: ${res.cancelledDaysCount}")
            }
            if (res.holidayNormalDaysCount > 0 || res.holidayDoubleDaysCount > 0 || res.holidayCustomDaysCount > 0) {
                val totalHolidays = res.holidayNormalDaysCount + res.holidayDoubleDaysCount + res.holidayCustomDaysCount
                appendLine("🟡 Jours fériés: $totalHolidays (${res.formattedHolidaySalaries})")
            }
            appendLine("🚚 Postes fixes: ${res.formattedFixedPosts}")
            appendLine("➕ Montant additionnel à prévoir: ${res.formattedAdditional}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("💰 PRÉVISION FINALE: ${res.formattedFinalForecast}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Généré avec l'application Prévision Paie (Hors-ligne)")
        }
    }

    fun buildDetailedBreakdownText(): String {
        val q = activeQuinzaine.value ?: return ""
        val res = calculationResult.value

        return buildString {
            appendLine(buildShareSummaryText())
            appendLine()
            appendLine("🔍 DÉTAIL DU CALCUL :")
            appendLine("1. Groupes de travailleurs (${res.formattedNormalSalaries}) :")
            res.groupBreakdowns.forEach { group ->
                appendLine("  • ${group.groupName} (${group.paymentType.labelFr}): ${group.formulaText}")
            }
            if (res.hourlyBreakdowns.isNotEmpty()) {
                appendLine("2. Équipes horaires dédiées (${res.formattedHourlySalaries}) :")
                res.hourlyBreakdowns.forEach { hw ->
                    appendLine("  • ${hw.name}: ${hw.formulaText}")
                }
            }
            if (res.fixedPostBreakdowns.isNotEmpty()) {
                appendLine("3. Postes fixes (${res.formattedFixedPosts}) :")
                res.fixedPostBreakdowns.forEach { fp ->
                    appendLine("  • ${fp.name} (${fp.category}): ${fp.formulaText}")
                }
            }
            appendLine()
            appendLine("Calcul récapitulatif:")
            appendLine("${res.formattedCurrentAmount} (actuel) + ${res.formattedAdditional} (prévu) = ${res.formattedFinalForecast}")
        }
    }

    fun shareText(context: Context, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    // -------------------------------------------------------------
    // EXPORT PDF: PRÉVISION COMPLETE, TRANSPORT & BONS
    // -------------------------------------------------------------
    fun exportPrevisionToPdf(context: Context): File? {
        val q = activeQuinzaine.value ?: return null
        val calc = calculationResult.value
        val groups = workerGroups.value
        val hourly = hourlyWorkers.value
        val fixed = fixedPosts.value
        val trans = transports.value
        val bonList = bons.value

        return PrevisionPdfGenerator.generatePrevisionPdf(
            context = context,
            quinzaine = q,
            calculationResult = calc,
            workerGroups = groups,
            hourlyWorkers = hourly,
            fixedPosts = fixed,
            transports = trans,
            bons = bonList
        )
    }

    fun exportAndSharePrevisionPdf(context: Context) {
        val file = exportPrevisionToPdf(context) ?: return
        val q = activeQuinzaine.value
        PrevisionPdfGenerator.sharePdf(context, file, "Prévision Paie - ${q?.title ?: ""}")
    }

    fun exportAndViewPrevisionPdf(context: Context) {
        val file = exportPrevisionToPdf(context) ?: return
        PrevisionPdfGenerator.viewPdf(context, file)
    }

    // -------------------------------------------------------------
    // SECTION INDÉPENDANTE: PRÉVISION TRANSPORT
    // Strict isolation: does NOT modify Quinzaine final forecast
    // -------------------------------------------------------------
    fun addTransport(
        name: String,
        effectif: Int,
        pricePerPerson: Double,
        daysCount: Int = 1,
        note: String = ""
    ) {
        val qId = _activeQuinzaineId.value ?: return
        viewModelScope.launch {
            repository.insertTransport(
                TransportEntity(
                    quinzaineId = qId,
                    name = name.ifBlank { "Transport" },
                    effectif = effectif,
                    pricePerPerson = pricePerPerson,
                    daysCount = if (daysCount > 0) daysCount else 1,
                    note = note
                )
            )
        }
    }

    fun updateTransport(transport: TransportEntity) {
        viewModelScope.launch {
            repository.updateTransport(transport)
        }
    }

    fun deleteTransport(transport: TransportEntity) {
        viewModelScope.launch {
            repository.deleteTransport(transport)
        }
    }

    fun duplicateTransport(transport: TransportEntity) {
        viewModelScope.launch {
            repository.insertTransport(
                transport.copy(
                    id = 0,
                    name = "${transport.name} (copie)"
                )
            )
        }
    }

    fun buildShareTransportText(): String {
        val q = activeQuinzaine.value ?: return ""
        val list = transports.value
        val totalEffectif = list.sumOf { it.effectif }
        val totalMontant = list.sumOf { it.totalAmount }

        return buildString {
            appendLine("🚍 PRÉVISION TRANSPORT (INDÉPENDANT)")
            appendLine("Quinzaine : ${q.title}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            list.forEach { t ->
                val daysStr = if (t.daysCount > 1) " × ${t.daysCount} j" else ""
                val amountStr = String.format(java.util.Locale.US, "%,.2f", t.totalAmount)
                appendLine("• ${t.name} : ${t.effectif} pers. × ${t.pricePerPerson} DH$daysStr = $amountStr DH")
                if (t.note.isNotBlank()) appendLine("  Note : ${t.note}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("👥 TOTAL EFFECTIF TRANSPORT : $totalEffectif personnes")
            appendLine("💰 TOTAL PRÉVISION TRANSPORT : ${String.format(java.util.Locale.US, "%,.2f", totalMontant)} DH")
            appendLine()
            appendLine("⚠️ Ce calcul est indépendant et n'est pas inclus dans la prévision de la quinzaine.")
        }
    }

    // -------------------------------------------------------------
    // SECTION INDÉPENDANTE: BONS
    // Informational tracking only: NO forecast, NO payroll impact
    // -------------------------------------------------------------
    fun addBon(
        bonNumber: String,
        date: String,
        description: String,
        quantityOrEffectif: Double,
        amount: Double,
        note: String = ""
    ) {
        val qId = _activeQuinzaineId.value ?: return
        viewModelScope.launch {
            repository.insertBon(
                BonEntity(
                    quinzaineId = qId,
                    bonNumber = bonNumber.ifBlank { "BON" },
                    date = date.ifBlank { DateHelper.today() },
                    description = description,
                    quantityOrEffectif = quantityOrEffectif,
                    amount = amount,
                    note = note
                )
            )
        }
    }

    fun updateBon(bon: BonEntity) {
        viewModelScope.launch {
            repository.updateBon(bon)
        }
    }

    fun deleteBon(bon: BonEntity) {
        viewModelScope.launch {
            repository.deleteBon(bon)
        }
    }

    fun buildShareBonsText(): String {
        val q = activeQuinzaine.value ?: return ""
        val list = bons.value
        val totalMontant = list.sumOf { it.amount }

        return buildString {
            appendLine("📄 REGISTRE DES BONS (SUIVI)")
            appendLine("Quinzaine : ${q.title}")
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            list.forEach { b ->
                val amountStr = String.format(java.util.Locale.US, "%,.2f", b.amount)
                appendLine("• [${b.bonNumber}] ${b.date} - ${b.description}")
                appendLine("  Quantité / Effectif : ${b.quantityOrEffectif} | Montant : $amountStr DH")
                if (b.note.isNotBlank()) appendLine("  Note : ${b.note}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📋 NOMBRE TOTAL DE BONS : ${list.size}")
            appendLine("💰 MONTANT TOTAL DES BONS : ${String.format(java.util.Locale.US, "%,.2f", totalMontant)} DH")
            appendLine()
            appendLine("⚠️ Informations de suivi uniquement — aucune prévision ni impact sur la paie.")
        }
    }
}
