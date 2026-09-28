package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateHelper {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val frenchFullFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH)
    private val frenchShortFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
    private val frenchDayMonthFormat = SimpleDateFormat("EEE d MMM", Locale.FRENCH)

    fun todayIso(): String {
        return isoFormat.format(Date())
    }

    fun today(): String {
        return todayIso()
    }

    fun parseIso(isoString: String): Calendar? {
        return try {
            val date = isoFormat.parse(isoString.trim()) ?: return null
            Calendar.getInstance().apply { time = date }
        } catch (e: Exception) {
            null
        }
    }

    fun formatIso(calendar: Calendar): String {
        return isoFormat.format(calendar.time)
    }

    fun formatFrenchShort(isoString: String): String {
        val cal = parseIso(isoString) ?: return isoString
        return frenchShortFormat.format(cal.time)
    }

    fun formatFrenchDayMonth(isoString: String): String {
        val cal = parseIso(isoString) ?: return isoString
        return frenchDayMonthFormat.format(cal.time).replaceFirstChar { it.uppercase() }
    }

    fun formatFrenchFull(isoString: String): String {
        val cal = parseIso(isoString) ?: return isoString
        return frenchFullFormat.format(cal.time).replaceFirstChar { it.uppercase() }
    }

    /**
     * Returns true if [dateToCheck] is strictly after [baseDate].
     */
    fun isAfter(dateToCheck: String, baseDate: String): Boolean {
        val c1 = parseIso(dateToCheck) ?: return false
        val c2 = parseIso(baseDate) ?: return false
        return c1.after(c2)
    }

    /**
     * Returns true if [dateToCheck] is on or before [limitDate].
     */
    fun isOnOrBefore(dateToCheck: String, limitDate: String): Boolean {
        val c1 = parseIso(dateToCheck) ?: return false
        val c2 = parseIso(limitDate) ?: return false
        return !c1.after(c2)
    }

    /**
     * Generates a sorted list of ISO date strings from [startDate] to [endDate] inclusive.
     */
    fun generateDateRange(startDate: String, endDate: String): List<String> {
        val startCal = parseIso(startDate) ?: return emptyList()
        val endCal = parseIso(endDate) ?: return emptyList()

        if (startCal.after(endCal)) return emptyList()

        val list = mutableListOf<String>()
        val current = startCal.clone() as Calendar

        while (!current.after(endCal)) {
            list.add(isoFormat.format(current.time))
            current.add(Calendar.DAY_OF_MONTH, 1)
        }
        return list
    }

    /**
     * Generates remaining dates to forecast:
     * strictly after [currentDate] and up to [endDate] inclusive.
     */
    fun generateRemainingDates(currentDate: String, endDate: String): List<String> {
        val all = generateDateRange(currentDate, endDate)
        return if (all.size > 1) all.drop(1) else emptyList()
    }

    /**
     * Checks if a date string is valid ISO YYYY-MM-DD
     */
    fun isValidIso(dateString: String): Boolean {
        return parseIso(dateString) != null
    }

    /**
     * Returns day of month number (e.g. 26)
     */
    fun getDayNumber(isoString: String): String {
        val cal = parseIso(isoString) ?: return ""
        return cal.get(Calendar.DAY_OF_MONTH).toString()
    }

    /**
     * Returns short day name in French (e.g. "Sam", "Dim")
     */
    fun getShortDayOfWeek(isoString: String): String {
        val cal = parseIso(isoString) ?: return ""
        return SimpleDateFormat("EEE", Locale.FRENCH).format(cal.time).replaceFirstChar { it.uppercase() }
    }
}
