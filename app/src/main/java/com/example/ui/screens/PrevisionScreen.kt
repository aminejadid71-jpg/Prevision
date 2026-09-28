package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalculationResult
import com.example.data.model.QuinzaineEntity
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.BrandSuccess
import com.example.util.DateHelper

@Composable
fun PrevisionScreen(
    quinzaine: QuinzaineEntity?,
    calculationResult: CalculationResult,
    onSaveDetails: (title: String, startDate: String, currentDate: String, endDate: String, currentAmount: Double, currency: String) -> Unit,
    onOpenBreakdown: () -> Unit,
    onShare: () -> Unit,
    onExportPdf: () -> Unit = {}
) {
    if (quinzaine == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune quinzaine active")
        }
        return
    }

    var title by remember(quinzaine.id) { mutableStateOf(quinzaine.title) }
    var startDate by remember(quinzaine.id) { mutableStateOf(quinzaine.startDate) }
    var currentDate by remember(quinzaine.id) { mutableStateOf(quinzaine.currentDate) }
    var endDate by remember(quinzaine.id) { mutableStateOf(quinzaine.endDate) }
    var currentAmountStr by remember(quinzaine.id) { mutableStateOf(quinzaine.currentAmount.toString()) }
    var currency by remember(quinzaine.id) { mutableStateOf(quinzaine.currency) }
    var hasSavedFeedback by remember { mutableStateOf(false) }

    // Instant update trigger on amount change
    LaunchedEffect(currentAmountStr) {
        val parsed = currentAmountStr.toDoubleOrNull()
        if (parsed != null && parsed != quinzaine.currentAmount) {
            onSaveDetails(title, startDate, currentDate, endDate, parsed, currency)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("prevision_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "📊 Configuration & Prévision",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary
            )
            Text(
                text = "Ajustez le montant actuel et les dates de la quinzaine",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Live calculation summary card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = com.example.ui.theme.BrandSecondaryContainer
                    ) {
                        Text(
                            text = "RÉSULTAT EN TEMPS RÉEL",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = com.example.ui.theme.BrandSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = calculationResult.formattedFinalForecast,
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = "${calculationResult.formattedCurrentAmount} (actuel) + ${calculationResult.formattedAdditional} (restant)",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Section: Formulaire Quinzaine
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Paramètres de la quinzaine",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nom de la quinzaine") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_quinzaine_title_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currentAmountStr,
                        onValueChange = { currentAmountStr = it },
                        label = { Text("Montant actuel déjà arrêté ($currency)") },
                        placeholder = { Text("Ex: 37102.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = BrandPrimary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_current_amount_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Date début") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = currentDate,
                            onValueChange = { currentDate = it },
                            label = { Text("Date arrêtée") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("Date de fin de quinzaine") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val amt = currentAmountStr.toDoubleOrNull() ?: 0.0
                            onSaveDetails(title, startDate, currentDate, endDate, amt, currency)
                            hasSavedFeedback = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_prevision_details_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (hasSavedFeedback) "Enregistré avec succès !" else "Mettre à jour la période")
                    }
                }
            }
        }

        // Section: Breakdown preview inside page
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
                        text = "Décomposition automatique du calcul",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Calcul transparent et automatique basé sur les règles et présences",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                    CalculationLineItem(
                        label = "Montant actuel arrêté",
                        amount = calculationResult.formattedCurrentAmount,
                        detail = "Au ${DateHelper.formatFrenchShort(currentDate)}"
                    )

                    CalculationLineItem(
                        label = "Salaires normaux restants",
                        amount = calculationResult.formattedNormalSalaries,
                        detail = "${calculationResult.normalWorkDaysCount} jours × ${calculationResult.totalWorkers} ouvriers"
                    )

                    if (calculationResult.holidayNormalDaysCount > 0 || calculationResult.holidayDoubleDaysCount > 0) {
                        CalculationLineItem(
                            label = "Jours fériés rémunérés",
                            amount = calculationResult.formattedHolidaySalaries,
                            detail = "${calculationResult.holidayNormalDaysCount} simple(s), ${calculationResult.holidayDoubleDaysCount} double(s)"
                        )
                    }

                    if (calculationResult.totalHourlySalaries > 0) {
                        CalculationLineItem(
                            label = "Équipes rémunérées à l'heure",
                            amount = calculationResult.formattedHourlySalaries,
                            detail = "${calculationResult.hourlyBreakdowns.size} équipe(s)"
                        )
                    }

                    CalculationLineItem(
                        label = "Postes fixes",
                        amount = calculationResult.formattedFixedPosts,
                        detail = "${calculationResult.fixedPostBreakdowns.size} poste(s) configuré(s)"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PRÉVISION FINALE :",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandPrimary
                        )
                        Text(
                            text = calculationResult.formattedFinalForecast,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenBreakdown,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(imageVector = Icons.Default.Functions, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Voir formules")
                        }

                        Button(
                            onClick = onShare,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandSuccess)
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Partager")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onExportPdf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prevision_export_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Télécharger la Prévision PDF", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CalculationLineItem(
    label: String,
    amount: String,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
