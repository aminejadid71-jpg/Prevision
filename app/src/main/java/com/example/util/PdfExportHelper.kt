package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.BonEntity
import com.example.data.model.CalculationResult
import com.example.data.model.FixedPostEntity
import com.example.data.model.HourlyWorkerEntity
import com.example.data.model.QuinzaineEntity
import com.example.data.model.TransportEntity
import com.example.data.model.WorkerGroupEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrevisionPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 standard height in points (72 dpi)
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 559f
    private const val CONTENT_WIDTH = 523f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 806f
    private const val MAX_CONTENT_Y = 780f

    /**
     * Generates a professional PDF containing all Quinzaine payroll forecast details,
     * calculation breakdowns, worker groups, independent transport section, and independent bons section.
     */
    fun generatePrevisionPdf(
        context: Context,
        quinzaine: QuinzaineEntity,
        calculationResult: CalculationResult,
        workerGroups: List<WorkerGroupEntity>,
        hourlyWorkers: List<HourlyWorkerEntity>,
        fixedPosts: List<FixedPostEntity>,
        transports: List<TransportEntity>,
        bons: List<BonEntity>
    ): File {
        val pdfDocument = PdfDocument()

        // 1. Calculate total pages with a dry run
        val totalPages = calculateTotalPages(
            quinzaine,
            calculationResult,
            workerGroups,
            hourlyWorkers,
            fixedPosts,
            transports,
            bons
        )

        // 2. Perform the actual drawing pass
        val drawer = PdfDrawContext(
            pdfDocument = pdfDocument,
            totalPages = totalPages,
            quinzaine = quinzaine
        )

        drawer.startFirstPage()

        // Section: Header
        drawer.drawHeader(quinzaine)

        // Section: Informations de la quinzaine
        drawer.drawQuinzaineInfo(quinzaine, calculationResult)

        // Section: Résumé & Prévision finale hero box
        drawer.drawForecastHeroBox(calculationResult)

        // Section: Détail du calcul de la Prévision
        drawer.drawCalculationBreakdown(calculationResult)

        // Section: Groupes de travailleurs
        drawer.drawWorkerGroupsSection(calculationResult, workerGroups)

        // Section: Postes fixes / Heures complémentaires
        drawer.drawFixedPostsSection(calculationResult, fixedPosts, hourlyWorkers)

        // Section: 🚐 TRANSPORT (Strictly separated!)
        drawer.drawTransportSection(transports)

        // Section: 🧾 LES BONS (Strictly separated!)
        drawer.drawBonsSection(bons)

        // Section: Final Summary (3 separate totals, no combining!)
        drawer.drawFinalSummarySection(calculationResult, transports, bons)

        // Finish last page
        drawer.finishCurrentPage()

        // Save PDF to cache directory
        val cleanTitle = quinzaine.title.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "Quinzaine" }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val fileName = "Prevision_${cleanTitle}_$dateStamp.pdf"

        val exportDir = File(context.cacheDir, "previsions").apply { if (!exists()) mkdirs() }
        val pdfFile = File(exportDir, fileName)

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    /**
     * Pre-calculates the exact number of pages required for the PDF.
     */
    private fun calculateTotalPages(
        quinzaine: QuinzaineEntity,
        calculationResult: CalculationResult,
        workerGroups: List<WorkerGroupEntity>,
        hourlyWorkers: List<HourlyWorkerEntity>,
        fixedPosts: List<FixedPostEntity>,
        transports: List<TransportEntity>,
        bons: List<BonEntity>
    ): Int {
        var pageCount = 1
        var y = MARGIN_TOP

        fun advance(height: Float) {
            if (y + height > MAX_CONTENT_Y) {
                pageCount++
                y = MARGIN_TOP + 40f // Page header allowance on secondary pages
            }
            y += height
        }

        advance(85f) // Header
        advance(65f) // Quinzaine Info
        advance(65f) // Hero Box
        advance(95f) // Calculation breakdown

        // Worker Groups
        advance(30f) // Section title
        advance(22f) // Table header
        val groupsCount = if (workerGroups.isEmpty()) 1 else workerGroups.size
        advance(groupsCount * 22f + 10f)

        // Fixed Posts & Hourly
        if (fixedPosts.isNotEmpty() || hourlyWorkers.isNotEmpty()) {
            advance(30f) // Title
            advance(22f) // Table header
            advance((fixedPosts.size + hourlyWorkers.size) * 22f + 10f)
        }

        // Transport
        advance(32f) // Section title
        if (transports.isEmpty()) {
            advance(30f)
        } else {
            advance(22f) // Table header
            advance(transports.size * 22f + 32f) // rows + total
        }

        // Bons
        advance(32f) // Section title
        if (bons.isEmpty()) {
            advance(30f)
        } else {
            advance(22f) // Table header
            advance(bons.size * 22f + 32f) // rows + total
        }

        // Final Summary Block
        advance(110f)

        return pageCount
    }

    /**
     * Opens the standard Android chooser to share or send the generated PDF.
     */
    fun sharePdf(context: Context, pdfFile: File, title: String = "Prévision Paie") {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Voici le document de Prévision de Paie : ${pdfFile.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Partager la Prévision PDF via...")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Opens the generated PDF in an external viewer or print app.
     */
    fun viewPdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to chooser if no default viewer
            val chooser = Intent.createChooser(intent, "Ouvrir le PDF avec...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    // -------------------------------------------------------------
    // DRAWING HELPER CONTEXT
    // -------------------------------------------------------------
    private class PdfDrawContext(
        val pdfDocument: PdfDocument,
        val totalPages: Int,
        val quinzaine: QuinzaineEntity
    ) {
        var currentPageNumber: Int = 0
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var currentY: Float = MARGIN_TOP

        // Sharp high-fidelity Paints
        val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 10f
            isSubpixelText = true
            isLinearText = true
            isFilterBitmap = true
            isDither = true
        }

        val paintTextBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isSubpixelText = true
            isLinearText = true
            isFilterBitmap = true
            isDither = true
        }

        val paintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139) // Slate 500
            textSize = 9f
            isSubpixelText = true
            isLinearText = true
            isFilterBitmap = true
            isDither = true
        }

        val paintFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            isDither = true
        }

        val paintStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
            isDither = true
        }

        fun startFirstPage() {
            startNewPage()
        }

        fun startNewPage() {
            currentPageNumber++
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNumber).create()
            currentPage = pdfDocument.startPage(pageInfo)
            canvas = currentPage?.canvas
            currentY = MARGIN_TOP

            if (currentPageNumber > 1) {
                // Secondary page header
                canvas?.let { c ->
                    paintSecondary.textSize = 8.5f
                    c.drawText("PRÉVISION PAIE — ${quinzaine.title}", MARGIN_LEFT, currentY + 10f, paintSecondary)
                    paintStroke.color = Color.rgb(226, 232, 240)
                    c.drawLine(MARGIN_LEFT, currentY + 16f, MARGIN_RIGHT, currentY + 16f, paintStroke)
                }
                currentY += 28f
            }
        }

        fun ensureSpace(heightNeeded: Float) {
            if (currentY + heightNeeded > MAX_CONTENT_Y) {
                drawPageFooter()
                pdfDocument.finishPage(currentPage)
                startNewPage()
            }
        }

        fun finishCurrentPage() {
            drawPageFooter()
            currentPage?.let { pdfDocument.finishPage(it) }
            currentPage = null
        }

        fun drawPageFooter() {
            val c = canvas ?: return
            val footerY = MARGIN_BOTTOM

            // Separator line
            paintStroke.color = Color.rgb(226, 232, 240)
            c.drawLine(MARGIN_LEFT, footerY - 14f, MARGIN_RIGHT, footerY - 14f, paintStroke)

            paintSecondary.textSize = 8f
            c.drawText(
                "Prévision Paie — Document officiel généré automatiquement",
                MARGIN_LEFT,
                footerY,
                paintSecondary
            )

            val pageStr = "Page $currentPageNumber / $totalPages"
            val pageStrWidth = paintSecondary.measureText(pageStr)
            c.drawText(pageStr, MARGIN_RIGHT - pageStrWidth, footerY, paintSecondary)
        }

        // ---------------------------------------------------------
        // SECTION 1: HEADER
        // ---------------------------------------------------------
        fun drawHeader(quinzaine: QuinzaineEntity) {
            val c = canvas ?: return
            val headerHeight = 78f
            val rect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + headerHeight)

            // Header Background Card (Deep Slate)
            paintFill.color = Color.rgb(15, 23, 42) // #0F172A
            c.drawRoundRect(rect, 8f, 8f, paintFill)

            // Green accent top bar
            paintFill.color = Color.rgb(34, 197, 94) // Emerald Green #22C55E
            val accentRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 4f)
            c.drawRoundRect(accentRect, 4f, 4f, paintFill)

            // Title
            paintTextBold.color = Color.WHITE
            paintTextBold.textSize = 17f
            c.drawText("PRÉVISION DE PAIE", MARGIN_LEFT + 16f, currentY + 28f, paintTextBold)

            // Subtitle
            paintSecondary.color = Color.rgb(203, 213, 225) // Slate 300
            paintSecondary.textSize = 9.5f
            c.drawText("Quinzaine : ${quinzaine.title}", MARGIN_LEFT + 16f, currentY + 44f, paintSecondary)

            val periodText = "Période : ${formatDate(quinzaine.startDate)} → ${formatDate(quinzaine.endDate)}"
            c.drawText(periodText, MARGIN_LEFT + 16f, currentY + 58f, paintSecondary)

            // Right-aligned Generation Date
            val genDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(Date())
            val dateLabel = "Généré le : $genDate"
            val dateLabelWidth = paintSecondary.measureText(dateLabel)
            c.drawText(dateLabel, MARGIN_RIGHT - 16f - dateLabelWidth, currentY + 28f, paintSecondary)

            val statusText = if (quinzaine.isCompleted) "Statut : Clôturée" else "Statut : En cours"
            val statusWidth = paintSecondary.measureText(statusText)
            c.drawText(statusText, MARGIN_RIGHT - 16f - statusWidth, currentY + 44f, paintSecondary)

            currentY += headerHeight + 14f
        }

        // ---------------------------------------------------------
        // SECTION 2: QUINZAINE INFO
        // ---------------------------------------------------------
        fun drawQuinzaineInfo(quinzaine: QuinzaineEntity, res: CalculationResult) {
            val c = canvas ?: return
            val blockHeight = 54f
            val rect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + blockHeight)

            paintFill.color = Color.rgb(248, 250, 252) // #F8FAFC
            c.drawRoundRect(rect, 6f, 6f, paintFill)
            paintStroke.color = Color.rgb(226, 232, 240)
            c.drawRoundRect(rect, 6f, 6f, paintStroke)

            val colW = CONTENT_WIDTH / 4f

            // Col 1: Date début
            drawKpiCell(c, MARGIN_LEFT + 10f, currentY + 12f, "DATE DÉBUT", formatDate(quinzaine.startDate))
            // Col 2: Date actuelle (arrêté)
            drawKpiCell(c, MARGIN_LEFT + colW, currentY + 12f, "DATE ARRÊTÉ", formatDate(quinzaine.currentDate))
            // Col 3: Date fin
            drawKpiCell(c, MARGIN_LEFT + colW * 2f, currentY + 12f, "DATE FIN", formatDate(quinzaine.endDate))
            // Col 4: Jours restants
            val daysDesc = "${res.normalWorkDaysCount} trav. • ${res.holidayNormalDaysCount + res.holidayDoubleDaysCount + res.holidayCustomDaysCount} fér."
            drawKpiCell(c, MARGIN_LEFT + colW * 3f, currentY + 12f, "JOURS RESTANTS", "${res.totalRemainingDaysCount} jours ($daysDesc)")

            currentY += blockHeight + 12f
        }

        private fun drawKpiCell(c: Canvas, x: Float, y: Float, label: String, value: String) {
            paintSecondary.color = Color.rgb(100, 116, 139)
            paintSecondary.textSize = 7.5f
            paintSecondary.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText(label, x, y + 8f, paintSecondary)

            paintTextBold.color = Color.rgb(15, 23, 42)
            paintTextBold.textSize = 9.5f
            c.drawText(value, x, y + 24f, paintTextBold)
        }

        // ---------------------------------------------------------
        // SECTION 3: HERO BOX — PRÉVISION FINALE
        // ---------------------------------------------------------
        fun drawForecastHeroBox(res: CalculationResult) {
            val c = canvas ?: return
            val boxHeight = 56f
            val rect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + boxHeight)

            // Light green container
            paintFill.color = Color.rgb(240, 253, 244) // Emerald-50 #F0FDF4
            c.drawRoundRect(rect, 8f, 8f, paintFill)
            paintStroke.color = Color.rgb(134, 239, 172) // Emerald-300 #86EFAC
            paintStroke.strokeWidth = 1.5f
            c.drawRoundRect(rect, 8f, 8f, paintStroke)
            paintStroke.strokeWidth = 1f

            // Left green badge
            val badgeRect = RectF(MARGIN_LEFT, currentY, MARGIN_LEFT + 6f, currentY + boxHeight)
            paintFill.color = Color.rgb(22, 163, 74) // Emerald-600 #16A34A
            c.drawRoundRect(badgeRect, 3f, 3f, paintFill)

            // Text Label
            paintTextBold.color = Color.rgb(21, 128, 61) // Emerald-700
            paintTextBold.textSize = 10f
            c.drawText("PRÉVISION FINALE DE LA QUINZAINE", MARGIN_LEFT + 18f, currentY + 22f, paintTextBold)

            paintSecondary.color = Color.rgb(71, 85, 105)
            paintSecondary.textSize = 8.5f
            val formulaDesc = "Montant actuel (${formatDh(res.currentAmount)}) + Reste à payer prévisionnel (${formatDh(res.totalRemainingAdditional)})"
            c.drawText(formulaDesc, MARGIN_LEFT + 18f, currentY + 38f, paintSecondary)

            // Big Number Right-Aligned
            val amountStr = "${formatDh(res.finalForecast)} DH"
            paintTextBold.color = Color.rgb(15, 23, 42)
            paintTextBold.textSize = 20f
            val amountWidth = paintTextBold.measureText(amountStr)
            c.drawText(amountStr, MARGIN_RIGHT - 18f - amountWidth, currentY + 35f, paintTextBold)

            currentY += boxHeight + 14f
        }

        // ---------------------------------------------------------
        // SECTION 4: CALCULATION BREAKDOWN
        // ---------------------------------------------------------
        fun drawCalculationBreakdown(res: CalculationResult) {
            val c = canvas ?: return
            ensureSpace(90f)

            drawSectionHeader(c, "DÉTAIL DU CALCUL DE LA PRÉVISION", "Composantes officielles")

            val tableHeight = 72f
            val rect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + tableHeight)
            paintFill.color = Color.rgb(255, 255, 255)
            c.drawRoundRect(rect, 6f, 6f, paintFill)
            paintStroke.color = Color.rgb(226, 232, 240)
            c.drawRoundRect(rect, 6f, 6f, paintStroke)

            val rows = listOf(
                "Montant arrêté actuel (base enregistrée)" to "${formatDh(res.currentAmount)} DH",
                "Salaires prévus (travailleurs normaux + fériés)" to "${formatDh(res.totalNormalSalaries + res.totalHolidaySalaries + res.totalHourlySalaries)} DH",
                "Postes fixes & charges complémentaires" to "${formatDh(res.totalFixedPosts)} DH",
                "PRÉVISION FINALE (TOTAL GÉNÉRAL PAIE)" to "${formatDh(res.finalForecast)} DH"
            )

            var rowY = currentY
            rows.forEachIndexed { i, (label, value) ->
                val isTotal = i == rows.size - 1
                if (isTotal) {
                    paintFill.color = Color.rgb(241, 245, 249)
                    c.drawRect(MARGIN_LEFT + 1f, rowY, MARGIN_RIGHT - 1f, rowY + 18f, paintFill)
                }

                if (isTotal) {
                    paintTextBold.color = Color.rgb(15, 23, 42)
                    paintTextBold.textSize = 9.5f
                    c.drawText(label, MARGIN_LEFT + 12f, rowY + 12.5f, paintTextBold)
                    val w = paintTextBold.measureText(value)
                    c.drawText(value, MARGIN_RIGHT - 12f - w, rowY + 12.5f, paintTextBold)
                } else {
                    paintText.color = Color.rgb(51, 65, 85)
                    paintText.textSize = 9f
                    c.drawText(label, MARGIN_LEFT + 12f, rowY + 12f, paintText)
                    val w = paintText.measureText(value)
                    c.drawText(value, MARGIN_RIGHT - 12f - w, rowY + 12f, paintText)
                }

                if (i < rows.size - 1) {
                    paintStroke.color = Color.rgb(241, 245, 249)
                    c.drawLine(MARGIN_LEFT, rowY + 18f, MARGIN_RIGHT, rowY + 18f, paintStroke)
                }
                rowY += 18f
            }

            currentY += tableHeight + 14f
        }

        // ---------------------------------------------------------
        // SECTION 5: GROUPES DE TRAVAILLEURS
        // ---------------------------------------------------------
        fun drawWorkerGroupsSection(res: CalculationResult, groups: List<WorkerGroupEntity>) {
            val c = canvas ?: return
            ensureSpace(60f)

            val totalWorkers = groups.sumOf { it.workerCount }
            drawSectionHeader(c, "GROUPES DE TRAVAILLEURS (${groups.size})", "Total effectif : $totalWorkers ouvriers")

            if (groups.isEmpty()) {
                drawEmptyCard(c, "Aucun groupe de travailleurs configuré")
                return
            }

            // Table Header
            drawTableHeader(
                c = c,
                colWidths = floatArrayOf(160f, 65f, 100f, 90f, 108f),
                colTitles = arrayOf("Groupe", "Effectif", "Taux", "Type", "Total Prévu"),
                alignments = arrayOf(Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.CENTER, Paint.Align.RIGHT)
            )

            // Table Rows
            val groupBreakdownMap = res.groupBreakdowns.associateBy { it.groupName }
            groups.forEachIndexed { i, group ->
                ensureSpace(22f)
                val br = groupBreakdownMap[group.name]
                val totalForGroup = br?.totalForGroup ?: (group.workerCount * group.dailyRate * res.normalWorkDaysCount)
                val rateStr = when (group.paymentType) {
                    com.example.data.model.PaymentType.DAILY -> "${formatDh(group.dailyRate)} DH/j"
                    com.example.data.model.PaymentType.HOURLY -> "${formatDh(group.hourlyRate)} DH/h"
                    com.example.data.model.PaymentType.CUSTOM_AMOUNT -> "${formatDh(group.customAmount)} DH (fixe)"
                }
                val typeStr = when (group.paymentType) {
                    com.example.data.model.PaymentType.DAILY -> "Journalier"
                    com.example.data.model.PaymentType.HOURLY -> "Horaire"
                    com.example.data.model.PaymentType.CUSTOM_AMOUNT -> "Forfaitaire"
                }

                drawTableRow(
                    c = c,
                    isEven = i % 2 == 0,
                    colWidths = floatArrayOf(160f, 65f, 100f, 90f, 108f),
                    colValues = arrayOf(group.name, "${group.workerCount}", rateStr, typeStr, "${formatDh(totalForGroup)} DH"),
                    alignments = arrayOf(Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.CENTER, Paint.Align.RIGHT)
                )
            }

            currentY += 10f
        }

        // ---------------------------------------------------------
        // SECTION 6: POSTES FIXES / COMPLÉMENTAIRES (IF ANY)
        // ---------------------------------------------------------
        fun drawFixedPostsSection(
            res: CalculationResult,
            fixedPosts: List<FixedPostEntity>,
            hourlyWorkers: List<HourlyWorkerEntity>
        ) {
            if (fixedPosts.isEmpty() && hourlyWorkers.isEmpty()) return
            val c = canvas ?: return
            ensureSpace(60f)

            drawSectionHeader(c, "POSTES FIXES & HEURES SUPPLÉMENTAIRES", "Total : ${formatDh(res.totalFixedPosts + res.totalHourlySalaries)} DH")

            drawTableHeader(
                c = c,
                colWidths = floatArrayOf(180f, 120f, 110f, 113f),
                colTitles = arrayOf("Poste / Désignation", "Catégorie / Mode", "Base", "Montant Prévu"),
                alignments = arrayOf(Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT)
            )

            var rowIndex = 0
            fixedPosts.forEach { fp ->
                ensureSpace(22f)
                val typeName = when (fp.type) {
                    com.example.data.model.FixedPostType.ONE_TIME -> "Forfait fixe"
                    com.example.data.model.FixedPostType.PER_DAY -> "Par jour"
                    com.example.data.model.FixedPostType.PER_WORKER -> "Par ouvrier"
                    com.example.data.model.FixedPostType.PER_DAY_PER_WORKER -> "Ouvrier / jour"
                    com.example.data.model.FixedPostType.CUSTOM_AMOUNT -> "Personnalisé"
                }
                val br = res.fixedPostBreakdowns.find { it.id == fp.id }
                val total = br?.calculatedTotal ?: fp.amount

                drawTableRow(
                    c = c,
                    isEven = rowIndex % 2 == 0,
                    colWidths = floatArrayOf(180f, 120f, 110f, 113f),
                    colValues = arrayOf(fp.name, "${fp.category} ($typeName)", "${formatDh(fp.amount)} DH", "${formatDh(total)} DH"),
                    alignments = arrayOf(Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT)
                )
                rowIndex++
            }

            hourlyWorkers.forEach { hw ->
                ensureSpace(22f)
                val total = hw.hoursWorked * hw.hourlyRate
                drawTableRow(
                    c = c,
                    isEven = rowIndex % 2 == 0,
                    colWidths = floatArrayOf(180f, 120f, 110f, 113f),
                    colValues = arrayOf(hw.name, "Heures supp. (${hw.hoursWorked} h)", "${formatDh(hw.hourlyRate)} DH/h", "${formatDh(total)} DH"),
                    alignments = arrayOf(Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT)
                )
                rowIndex++
            }

            currentY += 10f
        }

        // ---------------------------------------------------------
        // SECTION 7: 🚐 TRANSPORT (STRICTLY SEPARATED)
        // ---------------------------------------------------------
        fun drawTransportSection(transports: List<TransportEntity>) {
            val c = canvas ?: return
            ensureSpace(70f)

            val totalEffectif = transports.sumOf { it.effectif }
            val totalAmount = transports.sumOf { it.totalAmount }

            // Title with independent banner notice
            drawSectionHeader(
                c = c,
                title = "🚐 SECTION TRANSPORT (INDÉPENDANT)",
                subtitle = "Calcul isolé • STRICTEMENT NON INCLUSE dans la Prévision finale"
            )

            if (transports.isEmpty()) {
                drawEmptyCard(c, "Aucun transport enregistré")
                return
            }

            drawTableHeader(
                c = c,
                colWidths = floatArrayOf(180f, 80f, 90f, 70f, 103f),
                colTitles = arrayOf("Nom Transport", "Effectif", "Prix / pers", "Jours", "Total Ligne"),
                alignments = arrayOf(Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.CENTER, Paint.Align.RIGHT)
            )

            transports.forEachIndexed { i, t ->
                ensureSpace(22f)
                drawTableRow(
                    c = c,
                    isEven = i % 2 == 0,
                    colWidths = floatArrayOf(180f, 80f, 90f, 70f, 103f),
                    colValues = arrayOf(
                        t.name,
                        "${t.effectif} pers.",
                        "${formatDh(t.pricePerPerson)} DH",
                        "${t.daysCount} j",
                        "${formatDh(t.totalAmount)} DH"
                    ),
                    alignments = arrayOf(Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT, Paint.Align.CENTER, Paint.Align.RIGHT)
                )
            }

            // Subtotal Transport Bar
            ensureSpace(28f)
            val subtotalRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 22f)
            paintFill.color = Color.rgb(254, 243, 199) // Amber-100
            c.drawRoundRect(subtotalRect, 4f, 4f, paintFill)
            paintStroke.color = Color.rgb(251, 191, 36) // Amber-400
            c.drawRoundRect(subtotalRect, 4f, 4f, paintStroke)

            paintTextBold.color = Color.rgb(180, 83, 9) // Amber-700
            paintTextBold.textSize = 9.5f
            c.drawText("TOTAL TRANSPORT ($totalEffectif personnes au total) :", MARGIN_LEFT + 12f, currentY + 15f, paintTextBold)

            val totalStr = "${formatDh(totalAmount)} DH"
            val totalW = paintTextBold.measureText(totalStr)
            c.drawText(totalStr, MARGIN_RIGHT - 12f - totalW, currentY + 15f, paintTextBold)

            currentY += 28f
        }

        // ---------------------------------------------------------
        // SECTION 8: 🧾 LES BONS (STRICTLY SEPARATED)
        // ---------------------------------------------------------
        fun drawBonsSection(bons: List<BonEntity>) {
            val c = canvas ?: return
            ensureSpace(70f)

            val totalAmount = bons.sumOf { it.amount }

            drawSectionHeader(
                c = c,
                title = "🧾 REGISTRE DES BONS (SUIVI DOCUMENTAIRE)",
                subtitle = "Suivi informatif • Aucune prévision ni impact sur la paie"
            )

            if (bons.isEmpty()) {
                drawEmptyCard(c, "Aucun bon enregistré")
                return
            }

            drawTableHeader(
                c = c,
                colWidths = floatArrayOf(80f, 75f, 190f, 78f, 100f),
                colTitles = arrayOf("N° Bon", "Date", "Description", "Effectif / Qté", "Montant"),
                alignments = arrayOf(Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT)
            )

            bons.forEachIndexed { i, b ->
                ensureSpace(22f)
                val qtyStr = if (b.quantityOrEffectif % 1.0 == 0.0) b.quantityOrEffectif.toInt().toString() else b.quantityOrEffectif.toString()
                drawTableRow(
                    c = c,
                    isEven = i % 2 == 0,
                    colWidths = floatArrayOf(80f, 75f, 190f, 78f, 100f),
                    colValues = arrayOf(
                        b.bonNumber,
                        formatDate(b.date),
                        b.description.ifBlank { "Bon de dépense" },
                        qtyStr,
                        "${formatDh(b.amount)} DH"
                    ),
                    alignments = arrayOf(Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT)
                )
            }

            // Subtotal Bons Bar
            ensureSpace(28f)
            val subtotalRect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + 22f)
            paintFill.color = Color.rgb(241, 245, 249) // Slate-100
            c.drawRoundRect(subtotalRect, 4f, 4f, paintFill)
            paintStroke.color = Color.rgb(203, 213, 225) // Slate-300
            c.drawRoundRect(subtotalRect, 4f, 4f, paintStroke)

            paintTextBold.color = Color.rgb(15, 23, 42)
            paintTextBold.textSize = 9.5f
            c.drawText("TOTAL DES BONS (${bons.size} bon(s) répertorié(s)) :", MARGIN_LEFT + 12f, currentY + 15f, paintTextBold)

            val totalStr = "${formatDh(totalAmount)} DH"
            val totalW = paintTextBold.measureText(totalStr)
            c.drawText(totalStr, MARGIN_RIGHT - 12f - totalW, currentY + 15f, paintTextBold)

            currentY += 28f
        }

        // ---------------------------------------------------------
        // SECTION 9: FINAL SUMMARY — 3 STRICTLY SEPARATE TOTALS
        // ---------------------------------------------------------
        fun drawFinalSummarySection(
            res: CalculationResult,
            transports: List<TransportEntity>,
            bons: List<BonEntity>
        ) {
            val c = canvas ?: return
            ensureSpace(95f)

            val totalTransport = transports.sumOf { it.totalAmount }
            val totalBons = bons.sumOf { it.amount }

            val boxHeight = 84f
            val rect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + boxHeight)

            paintFill.color = Color.rgb(255, 255, 255)
            c.drawRoundRect(rect, 8f, 8f, paintFill)
            paintStroke.color = Color.rgb(203, 213, 225)
            paintStroke.strokeWidth = 1.2f
            c.drawRoundRect(rect, 8f, 8f, paintStroke)
            paintStroke.strokeWidth = 1f

            // Card title
            paintTextBold.color = Color.rgb(15, 23, 42)
            paintTextBold.textSize = 10.5f
            c.drawText("SYNTHÈSE GÉNÉRALE — 3 TOTAUX DISTINCTS", MARGIN_LEFT + 14f, currentY + 18f, paintTextBold)

            // 3 Column Totals
            val colW = CONTENT_WIDTH / 3f

            // 1. Prévision Finale
            val box1 = RectF(MARGIN_LEFT + 10f, currentY + 28f, MARGIN_LEFT + colW - 5f, currentY + boxHeight - 10f)
            paintFill.color = Color.rgb(240, 253, 244) // Emerald-50
            c.drawRoundRect(box1, 6f, 6f, paintFill)
            paintStroke.color = Color.rgb(134, 239, 172)
            c.drawRoundRect(box1, 6f, 6f, paintStroke)

            paintSecondary.color = Color.rgb(21, 128, 61)
            paintSecondary.textSize = 7.5f
            paintSecondary.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText("PRÉVISION FINALE", MARGIN_LEFT + 18f, currentY + 42f, paintSecondary)

            paintTextBold.color = Color.rgb(21, 128, 61)
            paintTextBold.textSize = 12.5f
            c.drawText("${formatDh(res.finalForecast)} DH", MARGIN_LEFT + 18f, currentY + 60f, paintTextBold)

            // 2. Total Transport
            val box2 = RectF(MARGIN_LEFT + colW + 5f, currentY + 28f, MARGIN_LEFT + colW * 2f - 5f, currentY + boxHeight - 10f)
            paintFill.color = Color.rgb(254, 243, 199) // Amber-50
            c.drawRoundRect(box2, 6f, 6f, paintFill)
            paintStroke.color = Color.rgb(251, 191, 36)
            c.drawRoundRect(box2, 6f, 6f, paintStroke)

            paintSecondary.color = Color.rgb(180, 83, 9)
            paintSecondary.textSize = 7.5f
            paintSecondary.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText("TOTAL TRANSPORT", MARGIN_LEFT + colW + 13f, currentY + 42f, paintSecondary)

            paintTextBold.color = Color.rgb(180, 83, 9)
            paintTextBold.textSize = 12.5f
            c.drawText("${formatDh(totalTransport)} DH", MARGIN_LEFT + colW + 13f, currentY + 60f, paintTextBold)

            // 3. Total Bons
            val box3 = RectF(MARGIN_LEFT + colW * 2f + 5f, currentY + 28f, MARGIN_RIGHT - 10f, currentY + boxHeight - 10f)
            paintFill.color = Color.rgb(241, 245, 249) // Slate-100
            c.drawRoundRect(box3, 6f, 6f, paintFill)
            paintStroke.color = Color.rgb(203, 213, 225)
            c.drawRoundRect(box3, 6f, 6f, paintStroke)

            paintSecondary.color = Color.rgb(71, 85, 105)
            paintSecondary.textSize = 7.5f
            paintSecondary.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            c.drawText("TOTAL DES BONS", MARGIN_LEFT + colW * 2f + 13f, currentY + 42f, paintSecondary)

            paintTextBold.color = Color.rgb(15, 23, 42)
            paintTextBold.textSize = 12.5f
            c.drawText("${formatDh(totalBons)} DH", MARGIN_LEFT + colW * 2f + 13f, currentY + 60f, paintTextBold)

            currentY += boxHeight + 14f

            // Disclaimer Note
            paintSecondary.textSize = 8f
            paintSecondary.color = Color.rgb(100, 116, 139)
            c.drawText(
                "⚠️ Note de conformité : Le Transport et les Bons sont strictement isolés de la Prévision Finale. Ces 3 montants ne sont jamais cumulés.",
                MARGIN_LEFT,
                currentY,
                paintSecondary
            )
            currentY += 16f
        }

        // ---------------------------------------------------------
        // GENERAL UI COMPONENT DRAWERS
        // ---------------------------------------------------------
        fun drawSectionHeader(c: Canvas, title: String, subtitle: String? = null) {
            paintTextBold.color = Color.rgb(15, 23, 42)
            paintTextBold.textSize = 10.5f
            c.drawText(title, MARGIN_LEFT, currentY + 12f, paintTextBold)

            if (subtitle != null) {
                val titleW = paintTextBold.measureText(title)
                paintSecondary.textSize = 8.5f
                paintSecondary.color = Color.rgb(100, 116, 139)
                c.drawText("• $subtitle", MARGIN_LEFT + titleW + 8f, currentY + 12f, paintSecondary)
            }

            currentY += 18f
        }

        fun drawTableHeader(
            c: Canvas,
            colWidths: FloatArray,
            colTitles: Array<String>,
            alignments: Array<Paint.Align>
        ) {
            val headerH = 18f
            val rect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + headerH)
            paintFill.color = Color.rgb(241, 245, 249) // Slate-100
            c.drawRoundRect(rect, 4f, 4f, paintFill)

            paintTextBold.textSize = 8.5f
            paintTextBold.color = Color.rgb(71, 85, 105)

            var curX = MARGIN_LEFT
            for (i in colTitles.indices) {
                val w = colWidths[i]
                val align = alignments[i]
                val textX = when (align) {
                    Paint.Align.LEFT -> curX + 6f
                    Paint.Align.CENTER -> curX + w / 2f
                    Paint.Align.RIGHT -> curX + w - 6f
                }
                paintTextBold.textAlign = align
                c.drawText(colTitles[i], textX, currentY + 12.5f, paintTextBold)
                curX += w
            }
            paintTextBold.textAlign = Paint.Align.LEFT

            currentY += headerH + 2f
        }

        fun drawTableRow(
            c: Canvas,
            isEven: Boolean,
            colWidths: FloatArray,
            colValues: Array<String>,
            alignments: Array<Paint.Align>
        ) {
            val rowH = 18f
            val rect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + rowH)

            if (isEven) {
                paintFill.color = Color.rgb(255, 255, 255)
            } else {
                paintFill.color = Color.rgb(248, 250, 252) // Slate-50
            }
            c.drawRect(rect, paintFill)

            paintStroke.color = Color.rgb(241, 245, 249)
            c.drawLine(MARGIN_LEFT, currentY + rowH, MARGIN_RIGHT, currentY + rowH, paintStroke)

            paintText.textSize = 8.5f
            paintText.color = Color.rgb(30, 41, 59)

            var curX = MARGIN_LEFT
            for (i in colValues.indices) {
                val w = colWidths[i]
                val align = alignments[i]
                val textX = when (align) {
                    Paint.Align.LEFT -> curX + 6f
                    Paint.Align.CENTER -> curX + w / 2f
                    Paint.Align.RIGHT -> curX + w - 6f
                }
                paintText.textAlign = align
                c.drawText(colValues[i], textX, currentY + 12.5f, paintText)
                curX += w
            }
            paintText.textAlign = Paint.Align.LEFT

            currentY += rowH
        }

        fun drawEmptyCard(c: Canvas, message: String) {
            val h = 26f
            val rect = RectF(MARGIN_LEFT, currentY, MARGIN_RIGHT, currentY + h)
            paintFill.color = Color.rgb(248, 250, 252)
            c.drawRoundRect(rect, 4f, 4f, paintFill)
            paintStroke.color = Color.rgb(226, 232, 240)
            c.drawRoundRect(rect, 4f, 4f, paintStroke)

            paintSecondary.textSize = 8.5f
            paintSecondary.color = Color.rgb(148, 163, 184)
            c.drawText(message, MARGIN_LEFT + 12f, currentY + 16.5f, paintSecondary)

            currentY += h + 10f
        }

        private fun formatDate(iso: String): String {
            return try {
                val cal = DateHelper.parseIso(iso)
                if (cal != null) {
                    SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(cal.time)
                } else {
                    iso
                }
            } catch (e: Exception) {
                iso
            }
        }

        private fun formatDh(value: Double): String {
            return String.format(Locale.US, "%,.2f", value)
        }
    }
}
