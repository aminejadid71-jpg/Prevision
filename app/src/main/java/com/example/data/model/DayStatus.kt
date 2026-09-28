package com.example.data.model

enum class DayStatus(val labelFr: String, val emoji: String, val badgeColorHex: Long) {
    WORK("Travail", "🟢", 0xFF16A34A),
    CANCELLED("Annulé", "🔴", 0xFFDC2626),
    HOLIDAY_PAID_NORMAL("Férié payé (1x)", "🟡", 0xFFEAB308),
    HOLIDAY_PAID_DOUBLE("Férié double (2x)", "🟠", 0xFFEA580C),
    HOLIDAY_CUSTOM_AMOUNT("Férié montant spécifié", "🟣", 0xFF8B5CF6),
    HOLIDAY_UNPAID("Férié non payé", "⚪", 0xFF9CA3AF),
    OFF("Non travaillé", "⚪", 0xFF6B7280);

    fun isWorkDay(): Boolean = this == WORK

    fun isHoliday(): Boolean = this == HOLIDAY_PAID_NORMAL ||
            this == HOLIDAY_PAID_DOUBLE ||
            this == HOLIDAY_CUSTOM_AMOUNT ||
            this == HOLIDAY_UNPAID

    fun holidayMultiplier(): Double = when (this) {
        HOLIDAY_PAID_NORMAL -> 1.0
        HOLIDAY_PAID_DOUBLE -> 2.0
        else -> 0.0
    }
}
