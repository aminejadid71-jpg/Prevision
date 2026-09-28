package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.FixedPostEntity
import com.example.data.model.FixedPostType
import com.example.data.model.HourlyWorkerEntity
import com.example.data.model.PaymentType
import com.example.data.model.WorkerGroupEntity
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.BrandSuccess
import com.example.util.DateHelper
import java.util.Calendar

@Composable
fun NewQuinzaineDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, startDate: String, currentDate: String, endDate: String, currentAmount: Double, currency: String) -> Unit
) {
    NewQuinzaineWizardDialog(onDismiss = onDismiss, onConfirm = onConfirm)
}

/**
 * Step-by-Step Guided Wizard for creating a new custom quinzaine.
 * Guides the user through:
 * Step 1: Titre & Période de la quinzaine
 * Step 2: Montant actuel déjà arrêté
 * Step 3: Validation & Création
 */
@Composable
fun NewQuinzaineWizardDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, startDate: String, currentDate: String, endDate: String, currentAmount: Double, currency: String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    // Defaults use today's real dynamic date, completely generic
    val todayCal = remember { Calendar.getInstance() }
    val todayIso = remember { DateHelper.todayIso() }

    val startCal = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, if (get(Calendar.DAY_OF_MONTH) <= 15) 1 else 16)
        }
    }
    val defaultStartDate = remember { DateHelper.formatIso(startCal) }

    val endCal = remember {
        Calendar.getInstance().apply {
            if (get(Calendar.DAY_OF_MONTH) <= 15) {
                set(Calendar.DAY_OF_MONTH, 15)
            } else {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }
        }
    }
    val defaultEndDate = remember { DateHelper.formatIso(endCal) }

    var title by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(defaultStartDate) }
    var currentDate by remember { mutableStateOf(todayIso) }
    var endDate by remember { mutableStateOf(defaultEndDate) }
    var currentAmountStr by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("DH") }

    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(BrandPrimary.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$step",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (step) {
                                1 -> "Étape 1 : Période"
                                2 -> "Étape 2 : Montant actuel"
                                else -> "Étape 3 : Confirmation"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Étape $step / 3",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (step) {
                    1 -> {
                        Text(
                            text = "Définissez le nom et les dates de votre quinzaine.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                validationError = null
                            },
                            label = { Text("Nom de la quinzaine") },
                            placeholder = { Text("Ex: 1ère quinzaine Octobre") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("quinzaine_title_input"),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = { startDate = it },
                                label = { Text("Date début") },
                                placeholder = { Text("AAAA-MM-JJ") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentDate,
                                onValueChange = { currentDate = it },
                                label = { Text("Date d'arrêté") },
                                placeholder = { Text("AAAA-MM-JJ") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("Date de fin de quinzaine") },
                            placeholder = { Text("AAAA-MM-JJ") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = BrandSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "La prévision calculera automatiquement les jours restants entre la date d'arrêté et la fin.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    2 -> {
                        Text(
                            text = "Entrez le montant de paie déjà arrêté ou réglé à ce jour.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = currentAmountStr,
                            onValueChange = {
                                currentAmountStr = it
                                validationError = null
                            },
                            label = { Text("Montant actuel arrêté ($currency)") },
                            placeholder = { Text("Ex: 0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = BrandPrimary)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("current_amount_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Devise (ex: DH, MAD, €)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Text(
                            text = "Si vous démarrez la quinzaine sans montants préalables, vous pouvez laisser 0.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    3 -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Récapitulatif de la nouvelle quinzaine :",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("• Titre : ${title.ifBlank { "Quinzaine sans titre" }}")
                                Text("• Période : $startDate au $endDate")
                                Text("• Arrêté au : $currentDate")
                                Text("• Montant actuel : ${currentAmountStr.ifBlank { "0.00" }} $currency")
                            }
                        }

                        Text(
                            text = "Après la création, vous pourrez ajouter vos travailleurs, vos postes fixes et configurer vos jours dans le calendrier.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            if (step < 3) {
                Button(
                    onClick = {
                        if (step == 1) {
                            if (title.isBlank()) {
                                title = "Quinzaine ${DateHelper.formatFrenchShort(startDate)}"
                            }
                            if (!DateHelper.isValidIso(startDate) || !DateHelper.isValidIso(currentDate) || !DateHelper.isValidIso(endDate)) {
                                validationError = "Format de date invalide (utiliser AAAA-MM-JJ)."
                                return@Button
                            }
                            step = 2
                        } else if (step == 2) {
                            step = 3
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Suivant")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            } else {
                Button(
                    onClick = {
                        val amount = currentAmountStr.toDoubleOrNull() ?: 0.0
                        onConfirm(title.ifBlank { "Quinzaine" }, startDate, currentDate, endDate, amount, currency)
                    },
                    modifier = Modifier.testTag("confirm_create_quinzaine_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandSuccess)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Créer la prévision")
                }
            }
        },
        dismissButton = {
            if (step > 1) {
                OutlinedButton(onClick = { step-- }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retour")
                }
            } else {
                OutlinedButton(onClick = onDismiss) {
                    Text("Annuler")
                }
            }
        }
    )
}

/**
 * Dialog to add or edit ANY worker group with 100% user-defined values and custom payment types.
 * Neutral placeholders only.
 */
@Composable
fun AddEditGroupDialog(
    initialGroup: WorkerGroupEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, count: Int, paymentType: PaymentType, dailyRate: Double, hourlyRate: Double, hoursWorked: Double, customAmount: Double, description: String) -> Unit
) {
    var name by remember { mutableStateOf(initialGroup?.name ?: "") }
    var countStr by remember { mutableStateOf(initialGroup?.workerCount?.toString() ?: "") }
    var paymentType by remember { mutableStateOf(initialGroup?.paymentType ?: PaymentType.DAILY) }

    var dailyRateStr by remember { mutableStateOf(initialGroup?.dailyRate?.takeIf { it > 0 }?.toString() ?: "") }
    var hourlyRateStr by remember { mutableStateOf(initialGroup?.hourlyRate?.takeIf { it > 0 }?.toString() ?: "") }
    var hoursWorkedStr by remember { mutableStateOf(initialGroup?.hoursWorked?.takeIf { it > 0 }?.toString() ?: "8.0") }
    var customAmountStr by remember { mutableStateOf(initialGroup?.customAmount?.takeIf { it > 0 }?.toString() ?: "") }
    var description by remember { mutableStateOf(initialGroup?.description ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val count = countStr.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = BrandPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (initialGroup == null) "Ajouter un groupe de travailleurs" else "Modifier le groupe")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Nom du groupe ou activité") },
                    placeholder = { Text("Ex: Cueilleurs, Taille, Équipe 1...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = countStr,
                    onValueChange = {
                        countStr = it
                        errorMessage = null
                    },
                    label = { Text("Nombre de travailleurs") },
                    placeholder = { Text("Ex: 10, 15, 25...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("worker_count_input"),
                    singleLine = true
                )

                Text(
                    text = "Mode de rémunération :",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Payment Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = paymentType == PaymentType.DAILY,
                        onClick = { paymentType = PaymentType.DAILY },
                        label = { Text("Journalier") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = paymentType == PaymentType.HOURLY,
                        onClick = { paymentType = PaymentType.HOURLY },
                        label = { Text("Horaire") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = paymentType == PaymentType.CUSTOM_AMOUNT,
                        onClick = { paymentType = PaymentType.CUSTOM_AMOUNT },
                        label = { Text("Forfait") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Mode-specific input fields
                when (paymentType) {
                    PaymentType.DAILY -> {
                        OutlinedTextField(
                            value = dailyRateStr,
                            onValueChange = {
                                dailyRateStr = it
                                errorMessage = null
                            },
                            label = { Text("Taux journalier (DH/jour par ouvrier)") },
                            placeholder = { Text("Ex: 95.00, 100.00, 120.00...") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("daily_rate_input"),
                            singleLine = true
                        )

                        val rate = dailyRateStr.toDoubleOrNull() ?: 0.0
                        if (count > 0 && rate > 0) {
                            Text(
                                text = "Masse journalière du groupe : ${String.format("%.2f DH/jour", count * rate)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary
                            )
                        }
                    }
                    PaymentType.HOURLY -> {
                        OutlinedTextField(
                            value = hourlyRateStr,
                            onValueChange = {
                                hourlyRateStr = it
                                errorMessage = null
                            },
                            label = { Text("Taux horaire (DH/heure)") },
                            placeholder = { Text("Ex: 12.00, 15.00...") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = hoursWorkedStr,
                            onValueChange = { hoursWorkedStr = it },
                            label = { Text("Nombre d'heures par jour") },
                            placeholder = { Text("Ex: 8.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    PaymentType.CUSTOM_AMOUNT -> {
                        OutlinedTextField(
                            value = customAmountStr,
                            onValueChange = {
                                customAmountStr = it
                                errorMessage = null
                            },
                            label = { Text("Montant forfaitaire global (DH)") },
                            placeholder = { Text("Ex: 2500.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes (Optionnel)") },
                    placeholder = { Text("Ex: Parcelle B, Responsable équipe...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Veuillez entrer un nom pour ce groupe."
                        return@Button
                    }
                    if (count <= 0) {
                        errorMessage = "Veuillez spécifier un nombre valide de travailleurs (au moins 1)."
                        return@Button
                    }

                    val dailyRate = dailyRateStr.toDoubleOrNull() ?: 0.0
                    val hourlyRate = hourlyRateStr.toDoubleOrNull() ?: 0.0
                    val hoursWorked = hoursWorkedStr.toDoubleOrNull() ?: 8.0
                    val customAmt = customAmountStr.toDoubleOrNull() ?: 0.0

                    if (paymentType == PaymentType.DAILY && dailyRate <= 0) {
                        errorMessage = "Veuillez entrer un taux journalier valide."
                        return@Button
                    }
                    if (paymentType == PaymentType.HOURLY && hourlyRate <= 0) {
                        errorMessage = "Veuillez entrer un taux horaire valide."
                        return@Button
                    }
                    if (paymentType == PaymentType.CUSTOM_AMOUNT && customAmt <= 0) {
                        errorMessage = "Veuillez entrer un montant forfaitaire valide."
                        return@Button
                    }

                    onConfirm(name, count, paymentType, dailyRate, hourlyRate, hoursWorked, customAmt, description)
                },
                modifier = Modifier.testTag("save_group_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialog to add/edit completely custom fixed posts.
 * Neutral placeholders only.
 */
@Composable
fun AddEditFixedPostDialog(
    initialPost: FixedPostEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, type: FixedPostType, amount: Double, qty: Int, days: Int, isLumpSum: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialPost?.name ?: "") }
    var category by remember { mutableStateOf(initialPost?.category ?: "") }
    var selectedType by remember { mutableStateOf(initialPost?.type ?: FixedPostType.ONE_TIME) }
    var amountStr by remember { mutableStateOf(initialPost?.amount?.takeIf { it > 0 }?.toString() ?: "") }
    var qtyStr by remember { mutableStateOf(initialPost?.quantity?.toString() ?: "") }
    var daysStr by remember { mutableStateOf(initialPost?.daysCount?.toString() ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.AttachMoney, contentDescription = null, tint = BrandPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (initialPost == null) "Ajouter un poste fixe" else "Modifier le poste fixe")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Nom du poste fixe") },
                    placeholder = { Text("Ex: Transport, Caporal, Prime...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Catégorie (Optionnel)") },
                    placeholder = { Text("Ex: Logistique, Encadrement, Matériel...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Mode de calcul :",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Calculation Mode Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedType == FixedPostType.ONE_TIME,
                            onClick = { selectedType = FixedPostType.ONE_TIME },
                            label = { Text("Forfait global") }
                        )
                        FilterChip(
                            selected = selectedType == FixedPostType.PER_DAY,
                            onClick = { selectedType = FixedPostType.PER_DAY },
                            label = { Text("Par jour") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedType == FixedPostType.PER_WORKER,
                            onClick = { selectedType = FixedPostType.PER_WORKER },
                            label = { Text("Par travailleur") }
                        )
                        FilterChip(
                            selected = selectedType == FixedPostType.PER_DAY_PER_WORKER,
                            onClick = { selectedType = FixedPostType.PER_DAY_PER_WORKER },
                            label = { Text("Par jour × ouvrier") }
                        )
                    }
                    FilterChip(
                        selected = selectedType == FixedPostType.CUSTOM_AMOUNT,
                        onClick = { selectedType = FixedPostType.CUSTOM_AMOUNT },
                        label = { Text("Montant personnalisé direct") }
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        errorMessage = null
                    },
                    label = {
                        Text(
                            when (selectedType) {
                                FixedPostType.ONE_TIME, FixedPostType.CUSTOM_AMOUNT -> "Montant total (DH)"
                                FixedPostType.PER_DAY -> "Montant par jour (DH/j)"
                                FixedPostType.PER_WORKER -> "Montant par travailleur (DH/pers)"
                                FixedPostType.PER_DAY_PER_WORKER -> "Tarif par ouvrier et par jour (DH)"
                            }
                        )
                    },
                    placeholder = { Text("Ex: 500.00, 20.00...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (selectedType == FixedPostType.PER_WORKER || selectedType == FixedPostType.PER_DAY_PER_WORKER) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Nombre de travailleurs (laisser vide = effectif total)") },
                        placeholder = { Text("Ex: Effectif automatique ou spécifique") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (selectedType == FixedPostType.PER_DAY || selectedType == FixedPostType.PER_DAY_PER_WORKER) {
                    OutlinedTextField(
                        value = daysStr,
                        onValueChange = { daysStr = it },
                        label = { Text("Nombre de jours (laisser vide = jours restants)") },
                        placeholder = { Text("Ex: Jours automatiques ou spécifiques") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Veuillez entrer un nom pour ce poste fixe."
                        return@Button
                    }
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        errorMessage = "Veuillez entrer un montant supérieur à 0."
                        return@Button
                    }
                    val qty = qtyStr.toIntOrNull() ?: 0
                    val days = daysStr.toIntOrNull() ?: 0
                    val isLumpSum = (selectedType == FixedPostType.ONE_TIME || selectedType == FixedPostType.CUSTOM_AMOUNT)
                    onConfirm(name, category.ifBlank { name }, selectedType, amount, qty, days, isLumpSum)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

/**
 * Dialog to add/edit direct hourly workers.
 */
@Composable
fun AddEditHourlyDialog(
    initialWorker: HourlyWorkerEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, hourlyRate: Double, hoursWorked: Double, targetAmount: Double?, description: String) -> Unit
) {
    var name by remember { mutableStateOf(initialWorker?.name ?: "") }
    var rateStr by remember { mutableStateOf(initialWorker?.hourlyRate?.takeIf { it > 0 }?.toString() ?: "") }
    var hoursStr by remember { mutableStateOf(initialWorker?.hoursWorked?.takeIf { it > 0 }?.toString() ?: "") }
    var targetStr by remember { mutableStateOf(initialWorker?.targetAmount?.takeIf { it > 0 }?.toString() ?: "") }
    var description by remember { mutableStateOf(initialWorker?.description ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    val rate = rateStr.toDoubleOrNull() ?: 0.0

    fun onTargetChanged(newTargetStr: String) {
        targetStr = newTargetStr
        val t = newTargetStr.toDoubleOrNull()
        if (t != null && t > 0 && rate > 0) {
            val calcHours = t / rate
            hoursStr = String.format("%.1f", calcHours).replace(',', '.')
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = BrandPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (initialWorker == null) "Ajouter ouvrier / équipe horaire" else "Modifier équipe horaire")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMessage = null
                    },
                    label = { Text("Désignation ou nom") },
                    placeholder = { Text("Ex: Conducteur tracteur, Emballage...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = rateStr,
                    onValueChange = {
                        rateStr = it
                        errorMessage = null
                    },
                    label = { Text("Taux horaire (DH/heure)") },
                    placeholder = { Text("Ex: 10.00, 15.00...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { onTargetChanged(it) },
                    label = { Text("Montant cible (Optionnel)") },
                    placeholder = { Text("Ex: 400 DH -> calcule automatiquement les heures") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = hoursStr,
                    onValueChange = { hoursStr = it },
                    label = { Text("Nombre d'heures travaillées") },
                    placeholder = { Text("Ex: 40.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val hours = hoursStr.toDoubleOrNull() ?: 0.0
                if (rate > 0 && hours > 0) {
                    val total = rate * hours
                    Text(
                        text = "Total prévu : ${String.format("%.2f DH", total)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = BrandSuccess
                    )
                }

                if (errorMessage != null) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Veuillez entrer un nom ou une désignation."
                        return@Button
                    }
                    val hourlyRate = rateStr.toDoubleOrNull() ?: 0.0
                    val hours = hoursStr.toDoubleOrNull() ?: 0.0
                    val target = targetStr.toDoubleOrNull()

                    if (hourlyRate <= 0) {
                        errorMessage = "Veuillez entrer un taux horaire valide."
                        return@Button
                    }
                    onConfirm(name, hourlyRate, hours, target, description)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Enregistrer")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
