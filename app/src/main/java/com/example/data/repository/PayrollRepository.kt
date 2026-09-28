package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.BonEntity
import com.example.data.model.CalendarDayEntity
import com.example.data.model.DayStatus
import com.example.data.model.FixedPostEntity
import com.example.data.model.FixedPostType
import com.example.data.model.HourlyWorkerEntity
import com.example.data.model.QuinzaineEntity
import com.example.data.model.TransportEntity
import com.example.data.model.WorkerGroupEntity
import com.example.util.DateHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class PayrollRepository(private val database: AppDatabase) {
    private val quinzaineDao = database.quinzaineDao()
    private val workerGroupDao = database.workerGroupDao()
    private val hourlyWorkerDao = database.hourlyWorkerDao()
    private val fixedPostDao = database.fixedPostDao()
    private val calendarDayDao = database.calendarDayDao()
    private val transportDao = database.transportDao()
    private val bonDao = database.bonDao()

    val allQuinzaines: Flow<List<QuinzaineEntity>> = quinzaineDao.getAllQuinzaines()

    fun getQuinzaine(id: Long): Flow<QuinzaineEntity?> = quinzaineDao.getQuinzaineById(id)

    fun getWorkerGroups(quinzaineId: Long): Flow<List<WorkerGroupEntity>> =
        workerGroupDao.getGroupsForQuinzaine(quinzaineId)

    fun getHourlyWorkers(quinzaineId: Long): Flow<List<HourlyWorkerEntity>> =
        hourlyWorkerDao.getHourlyWorkersForQuinzaine(quinzaineId)

    fun getFixedPosts(quinzaineId: Long): Flow<List<FixedPostEntity>> =
        fixedPostDao.getFixedPostsForQuinzaine(quinzaineId)

    fun getCalendarDays(quinzaineId: Long): Flow<List<CalendarDayEntity>> =
        calendarDayDao.getCalendarDaysForQuinzaine(quinzaineId)

    // Independant section: Prévision Transport
    fun getTransports(quinzaineId: Long): Flow<List<TransportEntity>> =
        transportDao.getTransportsForQuinzaine(quinzaineId)

    suspend fun insertTransport(transport: TransportEntity): Long =
        transportDao.insertTransport(transport)

    suspend fun updateTransport(transport: TransportEntity) =
        transportDao.updateTransport(transport)

    suspend fun deleteTransport(transport: TransportEntity) =
        transportDao.deleteTransport(transport)

    // Independant section: Bons
    fun getBons(quinzaineId: Long): Flow<List<BonEntity>> =
        bonDao.getBonsForQuinzaine(quinzaineId)

    suspend fun insertBon(bon: BonEntity): Long =
        bonDao.insertBon(bon)

    suspend fun updateBon(bon: BonEntity) =
        bonDao.updateBon(bon)

    suspend fun deleteBon(bon: BonEntity) =
        bonDao.deleteBon(bon)

    suspend fun createQuinzaine(quinzaine: QuinzaineEntity): Long {
        val id = quinzaineDao.insertQuinzaine(quinzaine)
        // Automatically populate default calendar days between startDate and endDate
        val dates = DateHelper.generateDateRange(quinzaine.startDate, quinzaine.endDate)
        val calendarDays = dates.map { date ->
            CalendarDayEntity(
                quinzaineId = id,
                date = date,
                statusString = DayStatus.WORK.name
            )
        }
        calendarDayDao.insertAllCalendarDays(calendarDays)
        return id
    }

    suspend fun updateQuinzaine(quinzaine: QuinzaineEntity) {
        quinzaineDao.updateQuinzaine(quinzaine.copy(updatedAt = System.currentTimeMillis()))
        // Ensure calendar days exist for the date range
        val existing = calendarDayDao.getCalendarDaysForQuinzaineDirect(quinzaine.id)
        val existingDates = existing.map { it.date }.toSet()
        val allDates = DateHelper.generateDateRange(quinzaine.startDate, quinzaine.endDate)
        val newDays = allDates.filter { it !in existingDates }.map { date ->
            CalendarDayEntity(
                quinzaineId = quinzaine.id,
                date = date,
                statusString = DayStatus.WORK.name
            )
        }
        if (newDays.isNotEmpty()) {
            calendarDayDao.insertAllCalendarDays(newDays)
        }
    }

    suspend fun deleteQuinzaine(id: Long) {
        quinzaineDao.deleteQuinzaineById(id)
    }

    suspend fun insertWorkerGroup(group: WorkerGroupEntity): Long {
        return workerGroupDao.insertGroup(group)
    }

    suspend fun updateWorkerGroup(group: WorkerGroupEntity) {
        workerGroupDao.updateGroup(group)
    }

    suspend fun deleteWorkerGroup(group: WorkerGroupEntity) {
        workerGroupDao.deleteGroup(group)
    }

    suspend fun insertHourlyWorker(worker: HourlyWorkerEntity): Long {
        return hourlyWorkerDao.insertHourlyWorker(worker)
    }

    suspend fun updateHourlyWorker(worker: HourlyWorkerEntity) {
        hourlyWorkerDao.updateHourlyWorker(worker)
    }

    suspend fun deleteHourlyWorker(worker: HourlyWorkerEntity) {
        hourlyWorkerDao.deleteHourlyWorker(worker)
    }

    suspend fun insertFixedPost(post: FixedPostEntity): Long {
        return fixedPostDao.insertFixedPost(post)
    }

    suspend fun updateFixedPost(post: FixedPostEntity) {
        fixedPostDao.updateFixedPost(post)
    }

    suspend fun deleteFixedPost(post: FixedPostEntity) {
        fixedPostDao.deleteFixedPost(post)
    }

    suspend fun setDayStatus(
        quinzaineId: Long,
        date: String,
        status: DayStatus,
        note: String = "",
        holidayCustomAmountPerWorker: Double = 0.0
    ) {
        calendarDayDao.insertOrUpdateCalendarDay(
            CalendarDayEntity(
                quinzaineId = quinzaineId,
                date = date,
                statusString = status.name,
                note = note,
                holidayCustomAmountPerWorker = holidayCustomAmountPerWorker
            )
        )
    }

    suspend fun duplicateQuinzaine(sourceId: Long, newTitle: String): Long {
        val source = quinzaineDao.getQuinzaineByIdDirect(sourceId) ?: return 0L
        val newQuinzaine = source.copy(
            id = 0,
            title = newTitle,
            isCompleted = false,
            updatedAt = System.currentTimeMillis()
        )
        val newId = quinzaineDao.insertQuinzaine(newQuinzaine)

        val groups = workerGroupDao.getGroupsForQuinzaineDirect(sourceId)
        val newGroups = groups.map { it.copy(id = 0, quinzaineId = newId) }
        workerGroupDao.insertAllGroups(newGroups)

        val fixed = fixedPostDao.getFixedPostsForQuinzaineDirect(sourceId)
        val newFixed = fixed.map { it.copy(id = 0, quinzaineId = newId) }
        fixedPostDao.insertAllFixedPosts(newFixed)

        val days = calendarDayDao.getCalendarDaysForQuinzaineDirect(sourceId)
        val newDays = days.map { it.copy(id = 0, quinzaineId = newId) }
        calendarDayDao.insertAllCalendarDays(newDays)

        return newId
    }

    suspend fun seedInitialDataIfEmpty(): Long? {
        val existing = allQuinzaines.firstOrNull()
        if (existing.isNullOrEmpty()) {
            return null
        }
        return existing.first().id
    }

    suspend fun seedOfficialScenario(): Long = loadDemoTestScenario()

    suspend fun loadDemoTestScenario(): Long {
        val quinzaine = QuinzaineEntity(
            title = "Septembre — 2ème quinzaine",
            startDate = "2026-09-16",
            currentDate = "2026-09-26",
            endDate = "2026-09-30",
            currentAmount = 37102.0,
            currency = "DH",
            notes = "Exemple de test avec 30 travailleurs et postes fixes"
        )
        val quinzaineId = quinzaineDao.insertQuinzaine(quinzaine)

        // Groups:
        // 8 workers × 99.96 DH/day
        // 22 workers × 90.87 DH/day
        val group1 = WorkerGroupEntity(
            quinzaineId = quinzaineId,
            name = "Groupe 1 (Cueillette)",
            workerCount = 8,
            dailyRate = 99.96
        )
        val group2 = WorkerGroupEntity(
            quinzaineId = quinzaineId,
            name = "Groupe 2 (Taille & Entretien)",
            workerCount = 22,
            dailyRate = 90.87
        )
        workerGroupDao.insertAllGroups(listOf(group1, group2))

        // Fixed posts: 1,281.51 DH
        val fixedPost = FixedPostEntity(
            quinzaineId = quinzaineId,
            name = "Transport & Postes fixes",
            category = "Transport",
            typeString = FixedPostType.ONE_TIME.name,
            amount = 1281.51,
            isDirectLumpSum = true
        )
        fixedPostDao.insertAllFixedPosts(listOf(fixedPost))

        // Calendar days: 16 to 30 Septembre
        val dates = DateHelper.generateDateRange(quinzaine.startDate, quinzaine.endDate)
        val days = dates.map { date ->
            val status = if (date == "2026-09-27") {
                DayStatus.CANCELLED
            } else {
                DayStatus.WORK
            }
            CalendarDayEntity(
                quinzaineId = quinzaineId,
                date = date,
                statusString = status.name,
                note = if (date == "2026-09-27") "Jour annulé (pluie/météo)" else ""
            )
        }
        calendarDayDao.insertAllCalendarDays(days)

        // Seed sample transports (independent calculation)
        val transport1 = TransportEntity(
            quinzaineId = quinzaineId,
            name = "Transport A (Minibus Oulad Teima)",
            effectif = 16,
            pricePerPerson = 20.0,
            daysCount = 1,
            note = "Equipe Cueillette nord"
        )
        val transport2 = TransportEntity(
            quinzaineId = quinzaineId,
            name = "Transport B (Fourgon Taroudant)",
            effectif = 10,
            pricePerPerson = 25.0,
            daysCount = 1,
            note = "Equipe Taille sud"
        )
        transportDao.insertAllTransports(listOf(transport1, transport2))

        // Seed sample bons (informational tracking only - no forecast, no payroll impact)
        val bon1 = BonEntity(
            quinzaineId = quinzaineId,
            bonNumber = "BON-001",
            date = "2026-09-20",
            description = "Achat pièces d'irrigation et raccords PVC",
            quantityOrEffectif = 1.0,
            amount = 500.0,
            note = "Quincaillerie agricole centrale"
        )
        val bon2 = BonEntity(
            quinzaineId = quinzaineId,
            bonNumber = "BON-002",
            date = "2026-09-23",
            description = "Carburant gasoil motopompe secteur 3",
            quantityOrEffectif = 1.0,
            amount = 700.0,
            note = "Station Afriquia"
        )
        val bon3 = BonEntity(
            quinzaineId = quinzaineId,
            bonNumber = "BON-003",
            date = "2026-09-25",
            description = "Sacs d'engrais foliaire d'appoint",
            quantityOrEffectif = 3.0,
            amount = 300.0,
            note = "Livraison directe ferme"
        )
        bonDao.insertAllBons(listOf(bon1, bon2, bon3))

        return quinzaineId
    }
}
