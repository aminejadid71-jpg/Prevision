package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalendarDayEntity
import com.example.data.model.CalculationResult
import com.example.data.model.DayStatus
import com.example.data.model.QuinzaineEntity
import com.example.ui.theme.BrandError
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.BrandSuccess
import com.example.ui.theme.BrandWarning
import com.example.util.DateHelper

@Composable
fun CalendarScreen(
    quinzaine: QuinzaineEntity?,
    calendarDays: List<CalendarDayEntity>,
    calculationResult: CalculationResult,
    onSetDayStatus: (date: String, status: DayStatus, note: String, holidayCustomAmount: Double) -> Unit
) {
    if (quinzaine == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune quinzaine active")
        }
        return
    }

    var selectedDayToEdit by remember { mutableStateOf<CalendarDayEntity?>(null) }

    // Map existing days by date
    val dayMap = remember(calendarDays) { calendarDays.associateBy { it.date } }
    val allQuinzaineDates = remember(quinzaine.startDate, quinzaine.endDate) {
        DateHelper.generateDateRange(quinzaine.startDate, quinzaine.endDate)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "📅 Calendrier de la quinzaine",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary
            )
            Text(
                text = "Chaque jour est personnalisable : Travail, Annulé, Férié payé (1x, 2x ou montant libre), Non travaillé",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary Bar Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Statistiques de la période restante à prévoir",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CalendarSummaryItem(label = "Travail", count = "${calculationResult.normalWorkDaysCount} j", color = BrandSuccess, emoji = "🟢")
                        CalendarSummaryItem(label = "Annulés", count = "${calculationResult.cancelledDaysCount} j", color = BrandError, emoji = "🔴")
                        CalendarSummaryItem(label = "Fériés 1x", count = "${calculationResult.holidayNormalDaysCount} j", color = BrandWarning, emoji = "🟡")
                        CalendarSummaryItem(label = "Fériés 2x/libres", count = "${calculationResult.holidayDoubleDaysCount + calculationResult.holidayCustomDaysCount} j", color = Color(0xFFEA580C), emoji = "🟠")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BrandSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Arrêté au ${DateHelper.formatFrenchShort(quinzaine.currentDate)}. Seuls les jours restants impactent la prévision.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // List of days
        items(allQuinzaineDates, key = { it }) { date ->
            val existing = dayMap[date]
            val status = existing?.status ?: DayStatus.WORK
            val note = existing?.note ?: ""
            val customAmt = existing?.holidayCustomAmountPerWorker ?: 0.0
            val isRemainingForForecast = DateHelper.isAfter(date, quinzaine.currentDate) && DateHelper.isOnOrBefore(date, quinzaine.endDate)
            val isPastOrCurrent = !isRemainingForForecast

            DayCard(
                date = date,
                status = status,
                note = note,
                customAmount = customAmt,
                isPastOrCurrent = isPastOrCurrent,
                isCurrentDate = (date == quinzaine.currentDate),
                onSelectStatus = { newStatus ->
                    onSetDayStatus(date, newStatus, note, customAmt)
                },
                onEditNote = {
                    selectedDayToEdit = CalendarDayEntity(
                        id = existing?.id ?: 0,
                        quinzaineId = quinzaine.id,
                        date = date,
                        statusString = status.name,
                        holidayCustomAmountPerWorker = customAmt,
                        note = note
                    )
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Detailed Day Customization Dialog
    selectedDayToEdit?.let { day ->
        var noteText by remember { mutableStateOf(day.note) }
        var tempStatus by remember { mutableStateOf(day.status) }
        var customAmountStr by remember { mutableStateOf(if (day.holidayCustomAmountPerWorker > 0) day.holidayCustomAmountPerWorker.toString() else "") }

        AlertDialog(
            onDismissRequest = { selectedDayToEdit = null },
            title = {
                Text("Configuration du ${DateHelper.formatFrenchShort(day.date)}")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Règle appliquée pour ce jour :",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    DayStatus.entries.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempStatus = s }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(s.emoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.labelFr,
                                fontWeight = if (tempStatus == s) FontWeight.Bold else FontWeight.Normal,
                                color = if (tempStatus == s) BrandPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (tempStatus == DayStatus.HOLIDAY_CUSTOM_AMOUNT) {
                        OutlinedTextField(
                            value = customAmountStr,
                            onValueChange = { customAmountStr = it },
                            label = { Text("Montant spécifique par ouvrier (DH)") },
                            placeholder = { Text("Ex: 150.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Motif ou remarque (ex: Pluie, Aïd, Récolte...)") },
                        placeholder = { Text("Ex: Météo défavorable...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val customAmt = customAmountStr.toDoubleOrNull() ?: 0.0
                        onSetDayStatus(day.date, tempStatus, noteText, customAmt)
                        selectedDayToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedDayToEdit = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun DayCard(
    date: String,
    status: DayStatus,
    note: String,
    customAmount: Double,
    isPastOrCurrent: Boolean,
    isCurrentDate: Boolean,
    onSelectStatus: (DayStatus) -> Unit,
    onEditNote: () -> Unit
) {
    val dayNumber = DateHelper.getDayNumber(date)
    val dayOfWeek = DateHelper.getShortDayOfWeek(date)
    val frenchFull = DateHelper.formatFrenchDayMonth(date)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("day_card_$date"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrentDate) BrandPrimary else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Date + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Date bubble
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                color = if (isCurrentDate) BrandPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dayOfWeek,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCurrentDate) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = dayNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentDate) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = frenchFull,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (isCurrentDate) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = BrandPrimary
                                ) {
                                    Text(
                                        text = "Aujourd'hui",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isPastOrCurrent) "Période déjà arrêtée" else "Période à prévoir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Current status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(status.badgeColorHex).copy(alpha = 0.15f)
                ) {
                    val statusText = if (status == DayStatus.HOLIDAY_CUSTOM_AMOUNT && customAmount > 0) {
                        "${status.emoji} Férié (${customAmount} DH/ouvrier)"
                    } else {
                        "${status.emoji} ${status.labelFr}"
                    }
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(status.badgeColorHex)
                    )
                }
            }

            if (note.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "📝 $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Status Chips for 1-tap switching
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusToggleChip(
                    emoji = "🟢",
                    label = "Travail",
                    selected = status == DayStatus.WORK,
                    onClick = { onSelectStatus(DayStatus.WORK) },
                    modifier = Modifier.weight(1f)
                )

                StatusToggleChip(
                    emoji = "🔴",
                    label = "Annulé",
                    selected = status == DayStatus.CANCELLED,
                    onClick = { onSelectStatus(DayStatus.CANCELLED) },
                    modifier = Modifier.weight(1f)
                )

                StatusToggleChip(
                    emoji = "🟡",
                    label = "Férié 1x",
                    selected = status == DayStatus.HOLIDAY_PAID_NORMAL,
                    onClick = { onSelectStatus(DayStatus.HOLIDAY_PAID_NORMAL) },
                    modifier = Modifier.weight(1f)
                )

                StatusToggleChip(
                    emoji = "🟠",
                    label = "Férié 2x",
                    selected = status == DayStatus.HOLIDAY_PAID_DOUBLE,
                    onClick = { onSelectStatus(DayStatus.HOLIDAY_PAID_DOUBLE) },
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onEditNote,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Règles avancées",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusToggleChip(
    emoji: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFF0F172A) else Color(0xFFF8FAFC),
        border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
            .height(34.dp)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$emoji $label",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else Color(0xFF334155)
            )
        }
    }
}

@Composable
private fun CalendarSummaryItem(
    label: String,
    count: String,
    color: Color,
    emoji: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$emoji $count", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
    }
}
