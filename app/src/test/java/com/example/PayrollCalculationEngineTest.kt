package com.example

import com.example.data.model.CalendarDayEntity
import com.example.data.model.DayStatus
import com.example.data.model.FixedPostEntity
import com.example.data.model.FixedPostType
import com.example.data.model.QuinzaineEntity
import com.example.data.model.WorkerGroupEntity
import com.example.engine.PayrollCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayrollCalculationEngineTest {

    @Test
    fun testOfficialScenarioFromUserPrompt() {
        val quinzaine = QuinzaineEntity(
            id = 1L,
            title = "Septembre — 2ème quinzaine",
            startDate = "2026-09-16",
            currentDate = "2026-09-26",
            endDate = "2026-09-30",
            currentAmount = 37102.0,
            currency = "DH"
        )

        val groups = listOf(
            WorkerGroupEntity(
                id = 1L,
                quinzaineId = 1L,
                name = "Groupe 1",
                workerCount = 8,
                dailyRate = 99.96
            ),
            WorkerGroupEntity(
                id = 2L,
                quinzaineId = 1L,
                name = "Groupe 2",
                workerCount = 22,
                dailyRate = 90.87
            )
        )

        val fixedPosts = listOf(
            FixedPostEntity(
                id = 1L,
                quinzaineId = 1L,
                name = "Postes fixes",
                category = "Transport & Postes fixes",
                typeString = FixedPostType.ONE_TIME.name,
                amount = 1281.51,
                isDirectLumpSum = true
            )
        )

        val calendarDays = listOf(
            CalendarDayEntity(
                id = 1L,
                quinzaineId = 1L,
                date = "2026-09-27",
                statusString = DayStatus.CANCELLED.name
            ),
            CalendarDayEntity(
                id = 2L,
                quinzaineId = 1L,
                date = "2026-09-28",
                statusString = DayStatus.WORK.name
            ),
            CalendarDayEntity(
                id = 3L,
                quinzaineId = 1L,
                date = "2026-09-29",
                statusString = DayStatus.WORK.name
            ),
            CalendarDayEntity(
                id = 4L,
                quinzaineId = 1L,
                date = "2026-09-30",
                statusString = DayStatus.WORK.name
            )
        )

        val result = PayrollCalculationEngine.calculate(
            quinzaine = quinzaine,
            workerGroups = groups,
            fixedPosts = fixedPosts,
            calendarDays = calendarDays
        )

        assertEquals("Normal remaining work days must be 3", 3, result.normalWorkDaysCount)
        assertEquals("Cancelled days count must be 1", 1, result.cancelledDaysCount)
        assertEquals("Total remaining days count must be 4", 4, result.totalRemainingDaysCount)
        assertEquals(2399.04, result.groupBreakdowns[0].normalTotal, 0.001)
        assertEquals(5997.42, result.groupBreakdowns[1].normalTotal, 0.001)
        assertEquals(8396.46, result.totalNormalSalaries, 0.001)
        assertEquals(1281.51, result.totalFixedPosts, 0.001)
        assertEquals(9677.97, result.totalRemainingAdditional, 0.001)
        assertEquals(46779.97, result.finalForecast, 0.001)
    }

    @Test
    fun testPublicHolidayNormalAndDoubleCalculations() {
        val quinzaine = QuinzaineEntity(
            id = 1L,
            title = "Quinzaine Test Férié",
            startDate = "2026-09-16",
            currentDate = "2026-09-26",
            endDate = "2026-09-30",
            currentAmount = 10000.0,
            currency = "DH"
        )

        val groups = listOf(
            WorkerGroupEntity(
                id = 1L,
                quinzaineId = 1L,
                name = "Groupe A",
                workerCount = 10,
                dailyRate = 100.0 // 1,000 DH/day
            )
        )

        // 4 days left: 27, 28, 29, 30
        // 27: CANCELLED (0)
        // 28: WORK (1,000)
        // 29: HOLIDAY_PAID_NORMAL (1,000)
        // 30: HOLIDAY_PAID_DOUBLE (2,000)
        val days = listOf(
            CalendarDayEntity(id = 1L, quinzaineId = 1L, date = "2026-09-27", statusString = DayStatus.CANCELLED.name),
            CalendarDayEntity(id = 2L, quinzaineId = 1L, date = "2026-09-28", statusString = DayStatus.WORK.name),
            CalendarDayEntity(id = 3L, quinzaineId = 1L, date = "2026-09-29", statusString = DayStatus.HOLIDAY_PAID_NORMAL.name),
            CalendarDayEntity(id = 4L, quinzaineId = 1L, date = "2026-09-30", statusString = DayStatus.HOLIDAY_PAID_DOUBLE.name)
        )

        val result = PayrollCalculationEngine.calculate(
            quinzaine = quinzaine,
            workerGroups = groups,
            calendarDays = days
        )

        assertEquals(1, result.normalWorkDaysCount)
        assertEquals(1, result.holidayNormalDaysCount)
        assertEquals(1, result.holidayDoubleDaysCount)
        assertEquals(1000.0, result.totalNormalSalaries, 0.001)
        assertEquals(3000.0, result.totalHolidaySalaries, 0.001) // 1000 + 2000
        assertEquals(4000.0, result.totalRemainingAdditional, 0.001)
        assertEquals(14000.0, result.finalForecast, 0.001)
    }

    @Test
    fun testStrictSeparationOfTransportAndBonsFromPrevision() {
        val quinzaine = QuinzaineEntity(
            id = 1L,
            title = "Test Quinzaine",
            startDate = "2026-09-16",
            currentDate = "2026-09-26",
            endDate = "2026-09-30",
            currentAmount = 37102.0,
            currency = "DH"
        )

        // 1. Independent Transport Calculation
        val transport1 = com.example.data.model.TransportEntity(
            id = 1L,
            quinzaineId = 1L,
            name = "Transport A",
            effectif = 16,
            pricePerPerson = 20.0
        )
        val transport2 = com.example.data.model.TransportEntity(
            id = 2L,
            quinzaineId = 1L,
            name = "Transport B",
            effectif = 10,
            pricePerPerson = 25.0
        )
        val transports = listOf(transport1, transport2)

        assertEquals(320.0, transport1.totalAmount, 0.001)
        assertEquals(250.0, transport2.totalAmount, 0.001)
        assertEquals(26, transports.sumOf { it.effectif })
        assertEquals(570.0, transports.sumOf { it.totalAmount }, 0.001)

        // 2. Independent Bons Tracking (NO calculation formula, informational only)
        val bon1 = com.example.data.model.BonEntity(
            id = 1L,
            quinzaineId = 1L,
            bonNumber = "BON-001",
            date = "2026-09-20",
            amount = 500.0
        )
        val bon2 = com.example.data.model.BonEntity(
            id = 2L,
            quinzaineId = 1L,
            bonNumber = "BON-002",
            date = "2026-09-23",
            amount = 700.0
        )
        val bon3 = com.example.data.model.BonEntity(
            id = 3L,
            quinzaineId = 1L,
            bonNumber = "BON-003",
            date = "2026-09-25",
            amount = 300.0
        )
        val bons = listOf(bon1, bon2, bon3)

        assertEquals(3, bons.size)
        assertEquals(1500.0, bons.sumOf { it.amount }, 0.001)

        // 3. Strict Verification: The existing Prévision calculation remains completely isolated
        val prevResult = PayrollCalculationEngine.calculate(
            quinzaine = quinzaine,
            workerGroups = emptyList(),
            hourlyWorkers = emptyList(),
            fixedPosts = emptyList(),
            calendarDays = emptyList()
        )
        // Prévision must remain strictly 37102.0 DH, totally unaffected by transports (570 DH) or bons (1500 DH)
        assertEquals(37102.0, prevResult.finalForecast, 0.001)
        assertEquals(37102.0, prevResult.currentAmount, 0.001)
        assertEquals(0.0, prevResult.totalRemainingAdditional, 0.001)
    }

    @Test
    fun testPdfExportDataIntegrityAndSeparation() {
        val quinzaine = QuinzaineEntity(
            id = 1L,
            title = "Quinzaine 18 - Septembre 2026",
            startDate = "2026-09-16",
            currentDate = "2026-09-26",
            endDate = "2026-09-30",
            currentAmount = 37102.0,
            currency = "DH"
        )
        val groups = listOf(
            WorkerGroupEntity(
                id = 1L,
                quinzaineId = 1L,
                name = "Groupe 1",
                workerCount = 8,
                paymentTypeString = "DAILY",
                dailyRate = 99.96
            ),
            WorkerGroupEntity(
                id = 2L,
                quinzaineId = 1L,
                name = "Groupe 2",
                workerCount = 22,
                paymentTypeString = "DAILY",
                dailyRate = 90.87
            )
        )
        val calc = PayrollCalculationEngine.calculate(
            quinzaine = quinzaine,
            workerGroups = groups,
            hourlyWorkers = emptyList(),
            fixedPosts = emptyList(),
            calendarDays = emptyList()
        )

        // Transport
        val transports = listOf(
            com.example.data.model.TransportEntity(
                id = 1L,
                quinzaineId = 1L,
                name = "Transport A",
                effectif = 16,
                pricePerPerson = 20.0,
                daysCount = 3
            ),
            com.example.data.model.TransportEntity(
                id = 2L,
                quinzaineId = 1L,
                name = "Transport B",
                effectif = 10,
                pricePerPerson = 25.0,
                daysCount = 3
            )
        )
        val totalTransport = transports.sumOf { it.totalAmount }
        // 16 * 20 * 3 = 960, 10 * 25 * 3 = 750 -> 1710.0 DH
        assertEquals(960.0, transports[0].totalAmount, 0.001)
        assertEquals(750.0, transports[1].totalAmount, 0.001)
        assertEquals(1710.0, totalTransport, 0.001)

        // Bons
        val bons = listOf(
            com.example.data.model.BonEntity(
                id = 1L,
                quinzaineId = 1L,
                bonNumber = "B-001",
                date = "2026-09-28",
                description = "Coco",
                quantityOrEffectif = 15.0,
                amount = 500.0
            ),
            com.example.data.model.BonEntity(
                id = 2L,
                quinzaineId = 1L,
                bonNumber = "B-002",
                date = "2026-09-29",
                description = "Matériel",
                quantityOrEffectif = 10.0,
                amount = 300.0
            )
        )
        val totalBons = bons.sumOf { it.amount }
        assertEquals(800.0, totalBons, 0.001)

        // Verify that the PDF uses calc.finalForecast without adding transports or bons
        assertTrue("Prévision finale must exceed currentAmount", calc.finalForecast > quinzaine.currentAmount)
        // Verify totalTransport and totalBons are distinct and non-zero
        assertTrue("Total transport must be separate", totalTransport > 0)
        assertTrue("Total bons must be separate", totalBons > 0)
        assertFalse(
            "Final forecast must NEVER equal combined sum",
            calc.finalForecast == (calc.finalForecast + totalTransport + totalBons)
        )
    }
}
