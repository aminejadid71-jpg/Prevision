package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CalculationResult
import com.example.data.model.QuinzaineEntity
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSuccess
import com.example.util.DateHelper

@Composable
fun CalculationBreakdownDialog(
    quinzaine: QuinzaineEntity,
    result: CalculationResult,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("calculation_breakdown_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(BrandPrimary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Functions,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Détail du calcul",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = quinzaine.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_breakdown_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Recap Card: Current Amount
                    BreakdownRowCard(
                        title = "Montant actuel (réalisé)",
                        amount = result.formattedCurrentAmount,
                        detail = "Arrêté au ${DateHelper.formatFrenchShort(quinzaine.currentDate)}",
                        accentColor = MaterialTheme.colorScheme.primary
                    )

                    // Section 1: Salaires journaliers normaux
                    BreakdownSectionCard(
                        title = "1. Salaires journaliers restants",
                        subtotal = result.formattedNormalSalaries,
                        itemCountText = "${result.normalWorkDaysCount} jours applicables × ${result.totalWorkers} ouvriers"
                    ) {
                        if (result.groupBreakdowns.isEmpty()) {
                            Text(
                                text = "Aucun groupe d'ouvriers configuré",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            result.groupBreakdowns.forEach { group ->
                                MathItemRow(
                                    label = group.groupName,
                                    mathFormula = "${group.workerCount} ouvriers × ${group.dailyRateFormatted} × ${group.workDaysCount} j",
                                    resultValue = CalculationResult.formatMoney(group.normalTotal, result.currency)
                                )
                            }
                        }
                    }

                    // Section 2: Jours fériés (si applicables)
                    if (result.holidayNormalDaysCount > 0 || result.holidayDoubleDaysCount > 0 || result.totalHolidaySalaries > 0) {
                        BreakdownSectionCard(
                            title = "2. Jours fériés",
                            subtotal = result.formattedHolidaySalaries,
                            itemCountText = "${result.holidayNormalDaysCount} payé(s) 1x, ${result.holidayDoubleDaysCount} payé(s) double"
                        ) {
                            result.groupBreakdowns.forEach { group ->
                                if (group.holidayNormalDays > 0) {
                                    MathItemRow(
                                        label = "${group.groupName} (Férié normal)",
                                        mathFormula = "${group.workerCount} × ${group.dailyRateFormatted} × ${group.holidayNormalDays} j",
                                        resultValue = CalculationResult.formatMoney(group.holidayNormalTotal, result.currency)
                                    )
                                }
                                if (group.holidayDoubleDays > 0) {
                                    MathItemRow(
                                        label = "${group.groupName} (Férié payé double)",
                                        mathFormula = "${group.workerCount} × (${group.dailyRateFormatted} × 2) × ${group.holidayDoubleDays} j",
                                        resultValue = CalculationResult.formatMoney(group.holidayDoubleTotal, result.currency)
                                    )
                                }
                            }
                        }
                    }

                    // Section 3: Équipes horaires (si applicables)
                    if (result.hourlyBreakdowns.isNotEmpty()) {
                        BreakdownSectionCard(
                            title = "3. Salaires horaires",
                            subtotal = result.formattedHourlySalaries,
                            itemCountText = "${result.hourlyBreakdowns.size} équipe(s)"
                        ) {
                            result.hourlyBreakdowns.forEach { hw ->
                                MathItemRow(
                                    label = hw.name,
                                    mathFormula = hw.formulaText,
                                    resultValue = CalculationResult.formatMoney(hw.calculatedTotal, result.currency)
                                )
                            }
                        }
                    }

                    // Section 4: Postes fixes
                    BreakdownSectionCard(
                        title = "4. Postes fixes",
                        subtotal = result.formattedFixedPosts,
                        itemCountText = "${result.fixedPostBreakdowns.size} poste(s) fixe(s)"
                    ) {
                        if (result.fixedPostBreakdowns.isEmpty()) {
                            Text(
                                text = "Aucun poste fixe (0.00 DH)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            result.fixedPostBreakdowns.forEach { fp ->
                                MathItemRow(
                                    label = "${fp.name} (${fp.category})",
                                    mathFormula = fp.formulaText,
                                    resultValue = CalculationResult.formatMoney(fp.calculatedTotal, result.currency)
                                )
                            }
                        }
                    }

                    // Total Remaining Additional Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total restant à prévoir :",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "+${result.formattedAdditional}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Grand Total Hero Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = com.example.ui.theme.BrandSecondaryContainer
                            ) {
                                Text(
                                    text = "PRÉVISION FINALE DE LA QUINZAINE",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.theme.BrandSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.formattedFinalForecast,
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${result.formattedCurrentAmount} (actuel) + ${result.formattedAdditional} (restant)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fermer")
                    }
                    Button(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("share_breakdown_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandSuccess)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Partager détail")
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownRowCard(
    title: String,
    amount: String,
    detail: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun BreakdownSectionCard(
    title: String,
    subtotal: String,
    itemCountText: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(text = itemCountText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = subtotal,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp)
            content()
        }
    }
}

@Composable
private fun MathItemRow(
    label: String,
    mathFormula: String,
    resultValue: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = resultValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = mathFormula,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = FontFamily.Monospace
        )
    }
}
