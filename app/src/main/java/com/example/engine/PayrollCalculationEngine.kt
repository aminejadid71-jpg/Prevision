package com.example.engine

import com.example.data.model.CalendarDayEntity
import com.example.data.model.CalculationResult
import com.example.data.model.DayStatus
import com.example.data.model.FixedPostBreakdown
import com.example.data.model.FixedPostEntity
import com.example.data.model.FixedPostType
import com.example.data.model.GroupBreakdown
import com.example.data.model.HourlyWorkerBreakdown
import com.example.data.model.HourlyWorkerEntity
import com.example.data.model.PaymentType
import com.example.data.model.QuinzaineEntity
import com.example.data.model.WorkerGroupEntity
import com.example.util.DateHelper
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

object PayrollCalculationEngine {

    /**
     * Executes the comprehensive calculation of the payroll forecast.
     * Fully flexible: supports any worker counts, any daily/hourly/custom payment types,
     * any holiday policies, and any fixed post rules.
     */
    fun calculate(
        quinzaine: QuinzaineEntity,
        workerGroups: List<WorkerGroupEntity>,
        hourlyWorkers: List<HourlyWorkerEntity> = emptyList(),
        fixedPosts: List<FixedPostEntity> = emptyList(),
        calendarDays: List<CalendarDayEntity> = emptyList()
    ): CalculationResult {
        val currency = quinzaine.currency.ifBlank { "DH" }
        val currentAmount = round2(quinzaine.currentAmount)

        // 1. Identify remaining calendar days strictly after quinzaine.currentDate and up to quinzaine.endDate
        val remainingDateStrings = DateHelper.generateRemainingDates(quinzaine.currentDate, quinzaine.endDate)
        val dayMap = calendarDays.associateBy { it.date }

        var normalWorkDaysCount = 0
        var cancelledDaysCount = 0
        var holidayNormalDaysCount = 0
        var holidayDoubleDaysCount = 0
        var holidayCustomDaysCount = 0
        var unpaidDaysCount = 0

        // Custom holiday details per day
        val customHolidayDays = mutableListOf<CalendarDayEntity>()

        for (date in remainingDateStrings) {
            val dayEntity = dayMap[date]
            val status = dayEntity?.status ?: DayStatus.WORK
            when (status) {
                DayStatus.WORK -> normalWorkDaysCount++
                DayStatus.CANCELLED -> cancelledDaysCount++
                DayStatus.HOLIDAY_PAID_NORMAL -> holidayNormalDaysCount++
                DayStatus.HOLIDAY_PAID_DOUBLE -> holidayDoubleDaysCount++
                DayStatus.HOLIDAY_CUSTOM_AMOUNT -> {
                    holidayCustomDaysCount++
                    if (dayEntity != null) {
                        customHolidayDays.add(dayEntity)
                    }
                }
                DayStatus.HOLIDAY_UNPAID, DayStatus.OFF -> unpaidDaysCount++
            }
        }

        val totalRemainingDaysCount = remainingDateStrings.size
        val totalWorkers = workerGroups.sumOf { it.workerCount }

        // 2. Worker Groups Calculation
        var totalNormalSalaries = 0.0
        var totalHolidaySalaries = 0.0
        val groupBreakdowns = mutableListOf<GroupBreakdown>()

        for (group in workerGroups) {
            val count = group.workerCount
            val paymentType = group.paymentType

            val dailyTotal: Double
            val normalTotal: Double
            val holidayNormalTotal: Double
            val holidayDoubleTotal: Double
            var holidayCustomTotal = 0.0
            val rateFormatted: String
            val formulaBuilder = StringBuilder()

            when (paymentType) {
                PaymentType.DAILY -> {
                    val rate = round2(group.dailyRate)
                    rateFormatted = String.format(Locale.FRENCH, "%.2f %s/j", rate, currency)
                    dailyTotal = round2(count * rate)
                    normalTotal = round2(dailyTotal * normalWorkDaysCount)

                    holidayNormalTotal = round2(dailyTotal * holidayNormalDaysCount * 1.0)
                    holidayDoubleTotal = round2(dailyTotal * holidayDoubleDaysCount * 2.0)

                    // Custom amount holidays
                    for (ch in customHolidayDays) {
                        val customRate = round2(ch.holidayCustomAmountPerWorker)
                        holidayCustomTotal = round2(holidayCustomTotal + (count * customRate))
                    }

                    formulaBuilder.append("$count ouvriers × $rate $currency × $normalWorkDaysCount j = $normalTotal $currency")
                    if (holidayNormalDaysCount > 0) {
                        formulaBuilder.append(" + $count × $rate × $holidayNormalDaysCount j (férié 1x) = $holidayNormalTotal $currency")
                    }
                    if (holidayDoubleDaysCount > 0) {
                        formulaBuilder.append(" + $count × $rate × 2 × $holidayDoubleDaysCount j (férié 2x) = $holidayDoubleTotal $currency")
                    }
                    if (holidayCustomDaysCount > 0) {
                        formulaBuilder.append(" + Fériés spécifiques = $holidayCustomTotal $currency")
                    }
                }
                PaymentType.HOURLY -> {
                    val rate = round2(group.hourlyRate)
                    val hours = if (group.hoursWorked > 0) round2(group.hoursWorked) else 8.0
                    rateFormatted = String.format(Locale.FRENCH, "%.2f %s/h (%.1f h/j)", rate, currency, hours)
                    // Daily total for all workers in group
                    dailyTotal = round2(count * rate * hours)
                    normalTotal = round2(dailyTotal * normalWorkDaysCount)
                    holidayNormalTotal = round2(dailyTotal * holidayNormalDaysCount * 1.0)
                    holidayDoubleTotal = round2(dailyTotal * holidayDoubleDaysCount * 2.0)

                    for (ch in customHolidayDays) {
                        val customRate = round2(ch.holidayCustomAmountPerWorker)
                        holidayCustomTotal = round2(holidayCustomTotal + (count * customRate))
                    }

                    formulaBuilder.append("$count ouvriers × $hours h × $rate $currency/h × $normalWorkDaysCount j = $normalTotal $currency")
                    if (holidayNormalDaysCount > 0) {
                        formulaBuilder.append(" + Férié 1x = $holidayNormalTotal $currency")
                    }
                    if (holidayDoubleDaysCount > 0) {
                        formulaBuilder.append(" + Férié 2x = $holidayDoubleTotal $currency")
                    }
                }
                PaymentType.CUSTOM_AMOUNT -> {
                    val customAmt = round2(group.customAmount)
                    rateFormatted = String.format(Locale.FRENCH, "%.2f %s (forfait)", customAmt, currency)
                    // If custom amount is lump sum for the group
                    dailyTotal = round2(customAmt / (if (normalWorkDaysCount > 0) normalWorkDaysCount else 1))
                    normalTotal = customAmt
                    holidayNormalTotal = 0.0
                    holidayDoubleTotal = 0.0

                    formulaBuilder.append("Forfait personnalisé = $normalTotal $currency")
                }
            }

            val totalForGroup = round2(normalTotal + holidayNormalTotal + holidayDoubleTotal + holidayCustomTotal)

            totalNormalSalaries = round2(totalNormalSalaries + normalTotal)
            totalHolidaySalaries = round2(totalHolidaySalaries + holidayNormalTotal + holidayDoubleTotal + holidayCustomTotal)

            groupBreakdowns.add(
                GroupBreakdown(
                    groupName = group.name,
                    workerCount = count,
                    paymentType = paymentType,
                    rateFormatted = rateFormatted,
                    dailyTotal = dailyTotal,
                    workDaysCount = normalWorkDaysCount,
                    normalTotal = normalTotal,
                    holidayNormalDays = holidayNormalDaysCount,
                    holidayNormalTotal = holidayNormalTotal,
                    holidayDoubleDays = holidayDoubleDaysCount,
                    holidayDoubleTotal = holidayDoubleTotal,
                    holidayCustomDays = holidayCustomDaysCount,
                    holidayCustomTotal = holidayCustomTotal,
                    totalForGroup = totalForGroup,
                    formulaText = formulaBuilder.toString(),
                    description = group.description
                )
            )
        }

        // 3. Hourly Workers Calculation (Direct / Dedicated entities)
        var totalHourlySalaries = 0.0
        val hourlyBreakdowns = mutableListOf<HourlyWorkerBreakdown>()

        for (hw in hourlyWorkers) {
            val rate = round2(hw.hourlyRate)
            val hours = round2(hw.hoursWorked)
            val subtotal = round2(rate * hours)
            totalHourlySalaries = round2(totalHourlySalaries + subtotal)

            val formulaText = "$hours h × $rate $currency/h = $subtotal $currency"
            hourlyBreakdowns.add(
                HourlyWorkerBreakdown(
                    id = hw.id,
                    name = hw.name,
                    hourlyRate = rate,
                    hoursWorked = hours,
                    calculatedTotal = subtotal,
                    formulaText = formulaText
                )
            )
        }

        // 4. Fixed Posts Calculation (Fully customized modes)
        var totalFixedPosts = 0.0
        val fixedBreakdowns = mutableListOf<FixedPostBreakdown>()

        for (fp in fixedPosts) {
            val baseAmount = round2(fp.amount)
            val calculatedTotal: Double
            val formulaText: String

            when (fp.type) {
                FixedPostType.ONE_TIME, FixedPostType.CUSTOM_AMOUNT -> {
                    calculatedTotal = baseAmount
                    formulaText = "Montant fixe = $calculatedTotal $currency"
                }
                FixedPostType.PER_DAY -> {
                    val days = if (fp.daysCount > 0) fp.daysCount else normalWorkDaysCount
                    calculatedTotal = round2(baseAmount * days)
                    formulaText = "$baseAmount $currency × $days j = $calculatedTotal $currency"
                }
                FixedPostType.PER_WORKER -> {
                    val qty = if (fp.quantity > 0) fp.quantity else totalWorkers
                    calculatedTotal = round2(baseAmount * qty)
                    formulaText = "$baseAmount $currency × $qty travailleurs = $calculatedTotal $currency"
                }
                FixedPostType.PER_DAY_PER_WORKER -> {
                    val qty = if (fp.quantity > 0) fp.quantity else totalWorkers
                    val days = if (fp.daysCount > 0) fp.daysCount else normalWorkDaysCount
                    calculatedTotal = round2(baseAmount * qty * days)
                    formulaText = "$baseAmount $currency × $qty travailleurs × $days j = $calculatedTotal $currency"
                }
            }

            totalFixedPosts = round2(totalFixedPosts + calculatedTotal)
            fixedBreakdowns.add(
                FixedPostBreakdown(
                    id = fp.id,
                    name = fp.name,
                    category = fp.category,
                    type = fp.type,
                    baseAmount = baseAmount,
                    quantity = fp.quantity,
                    daysCount = fp.daysCount,
                    calculatedTotal = calculatedTotal,
                    formulaText = formulaText
                )
            )
        }

        // 5. Final Grand Totals
        val totalRemainingAdditional = round2(
            totalNormalSalaries + totalHolidaySalaries + totalHourlySalaries + totalFixedPosts
        )
        val finalForecast = round2(currentAmount + totalRemainingAdditional)

        return CalculationResult(
            currentAmount = currentAmount,
            totalNormalSalaries = totalNormalSalaries,
            totalHolidaySalaries = totalHolidaySalaries,
            totalHourlySalaries = totalHourlySalaries,
            totalFixedPosts = totalFixedPosts,
            totalRemainingAdditional = totalRemainingAdditional,
            finalForecast = finalForecast,
            totalWorkers = totalWorkers,
            normalWorkDaysCount = normalWorkDaysCount,
            cancelledDaysCount = cancelledDaysCount,
            holidayNormalDaysCount = holidayNormalDaysCount,
            holidayDoubleDaysCount = holidayDoubleDaysCount,
            holidayCustomDaysCount = holidayCustomDaysCount,
            unpaidDaysCount = unpaidDaysCount,
            totalRemainingDaysCount = totalRemainingDaysCount,
            currency = currency,
            groupBreakdowns = groupBreakdowns,
            fixedPostBreakdowns = fixedBreakdowns,
            hourlyBreakdowns = hourlyBreakdowns
        )
    }

    fun round2(value: Double): Double {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }
}
