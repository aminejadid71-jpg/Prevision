package com.example.data.model

import java.util.Locale

data class GroupBreakdown(
    val groupName: String,
    val workerCount: Int,
    val paymentType: PaymentType,
    val rateFormatted: String,
    val dailyTotal: Double,
    val workDaysCount: Int,
    val normalTotal: Double,
    val holidayNormalDays: Int,
    val holidayNormalTotal: Double,
    val holidayDoubleDays: Int,
    val holidayDoubleTotal: Double,
    val holidayCustomDays: Int,
    val holidayCustomTotal: Double,
    val totalForGroup: Double,
    val formulaText: String,
    val description: String = ""
) {
    val dailyRateFormatted: String
        get() = rateFormatted
}

data class FixedPostBreakdown(
    val id: Long,
    val name: String,
    val category: String,
    val type: FixedPostType,
    val baseAmount: Double,
    val quantity: Int,
    val daysCount: Int,
    val calculatedTotal: Double,
    val formulaText: String
)

data class HourlyWorkerBreakdown(
    val id: Long,
    val name: String,
    val hourlyRate: Double,
    val hoursWorked: Double,
    val calculatedTotal: Double,
    val formulaText: String
)

data class CalculationResult(
    val currentAmount: Double,
    val totalNormalSalaries: Double,
    val totalHolidaySalaries: Double,
    val totalHourlySalaries: Double,
    val totalFixedPosts: Double,
    val totalRemainingAdditional: Double,
    val finalForecast: Double,
    val totalWorkers: Int,
    val normalWorkDaysCount: Int,
    val cancelledDaysCount: Int,
    val holidayNormalDaysCount: Int,
    val holidayDoubleDaysCount: Int,
    val holidayCustomDaysCount: Int = 0,
    val unpaidDaysCount: Int,
    val totalRemainingDaysCount: Int,
    val currency: String = "DH",
    val groupBreakdowns: List<GroupBreakdown> = emptyList(),
    val fixedPostBreakdowns: List<FixedPostBreakdown> = emptyList(),
    val hourlyBreakdowns: List<HourlyWorkerBreakdown> = emptyList()
) {
    val formattedCurrentAmount: String
        get() = formatMoney(currentAmount, currency)

    val formattedNormalSalaries: String
        get() = formatMoney(totalNormalSalaries, currency)

    val formattedHolidaySalaries: String
        get() = formatMoney(totalHolidaySalaries, currency)

    val formattedHourlySalaries: String
        get() = formatMoney(totalHourlySalaries, currency)

    val formattedFixedPosts: String
        get() = formatMoney(totalFixedPosts, currency)

    val formattedAdditional: String
        get() = formatMoney(totalRemainingAdditional, currency)

    val formattedFinalForecast: String
        get() = formatMoney(finalForecast, currency)

    companion object {
        fun formatMoney(amount: Double, currency: String = "DH"): String {
            return String.format(Locale.FRENCH, "%,.2f %s", amount, currency)
        }
    }
}
