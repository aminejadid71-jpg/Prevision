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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CalculationResult
import com.example.data.model.FixedPostEntity
import com.example.data.model.FixedPostType
import com.example.data.model.HourlyWorkerEntity
import com.example.data.model.PaymentType
import com.example.data.model.QuinzaineEntity
import com.example.data.model.WorkerGroupEntity
import com.example.ui.components.AddEditFixedPostDialog
import com.example.ui.components.AddEditGroupDialog
import com.example.ui.components.AddEditHourlyDialog
import com.example.ui.theme.BrandError
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.BrandSuccess
import com.example.ui.theme.BrandWarning

@Composable
fun WorkersScreen(
    quinzaine: QuinzaineEntity?,
    workerGroups: List<WorkerGroupEntity>,
    hourlyWorkers: List<HourlyWorkerEntity>,
    fixedPosts: List<FixedPostEntity>,
    calculationResult: CalculationResult,
    onAddGroup: (name: String, count: Int, paymentType: PaymentType, dailyRate: Double, hourlyRate: Double, hoursWorked: Double, customAmount: Double, description: String) -> Unit,
    onUpdateGroup: (WorkerGroupEntity) -> Unit,
    onDeleteGroup: (WorkerGroupEntity) -> Unit,
    onDuplicateGroup: (WorkerGroupEntity) -> Unit,
    onAddHourly: (name: String, rate: Double, hours: Double, target: Double?, description: String) -> Unit,
    onUpdateHourly: (HourlyWorkerEntity) -> Unit,
    onDeleteHourly: (HourlyWorkerEntity) -> Unit,
    onAddFixedPost: (name: String, category: String, type: FixedPostType, amount: Double, qty: Int, days: Int, isLumpSum: Boolean) -> Unit,
    onUpdateFixedPost: (FixedPostEntity) -> Unit,
    onDeleteFixedPost: (FixedPostEntity) -> Unit
) {
    if (quinzaine == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucune quinzaine active")
        }
        return
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var groupToEdit by remember { mutableStateOf<WorkerGroupEntity?>(null) }
    var isAddingGroup by remember { mutableStateOf(false) }

    var hourlyToEdit by remember { mutableStateOf<HourlyWorkerEntity?>(null) }
    var isAddingHourly by remember { mutableStateOf(false) }

    var fixedPostToEdit by remember { mutableStateOf<FixedPostEntity?>(null) }
    var isAddingFixedPost by remember { mutableStateOf(false) }

    val tabs = listOf("Groupes (${workerGroups.size})", "Horaires (${hourlyWorkers.size})", "Postes fixes (${fixedPosts.size})")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workers_screen")
    ) {
        // Top Header
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "👥 Travailleurs & Postes Fixes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary
            )
            Text(
                text = "Créez autant de groupes, de taux et de postes fixes que votre exploitation requiert",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Tabs
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // TAB 0: Worker Groups
                    item {
                        val dailyTotal = workerGroups.sumOf { it.dailySubtotal }
                        val totalWorkers = workerGroups.sumOf { it.workerCount }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Coût journalier normal estimé :",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format("%.2f %s/jour", dailyTotal, quinzaine.currency),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BrandSuccess.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$totalWorkers travailleurs",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandSuccess
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Groupes de travailleurs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { isAddingGroup = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_group_button")
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajouter un groupe")
                            }
                        }
                    }

                    if (workerGroups.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "Aucun groupe de travailleurs pour le moment. Cliquez sur 'Ajouter un groupe' pour définir votre premier groupe, son effectif et son taux.",
                                onAction = { isAddingGroup = true }
                            )
                        }
                    } else {
                        items(workerGroups, key = { it.id }) { group ->
                            WorkerGroupCard(
                                group = group,
                                remainingDays = calculationResult.normalWorkDaysCount,
                                currency = quinzaine.currency,
                                onEdit = { groupToEdit = group },
                                onDuplicate = { onDuplicateGroup(group) },
                                onDelete = { onDeleteGroup(group) }
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: Dedicated Hourly Workers
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
                                    text = "Travailleurs à l'heure dédiés",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Permet de saisir un taux horaire et de calculer automatiquement le nombre d'heures ou le montant cible.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Total prévu : ${calculationResult.formattedHourlySalaries}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Équipes rémunérées à l'heure",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { isAddingHourly = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajouter")
                            }
                        }
                    }

                    if (hourlyWorkers.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "Aucun travailleur horaire dédié. Idéal pour les conducteurs de machines, emballage ou missions spécifiques.",
                                onAction = { isAddingHourly = true }
                            )
                        }
                    } else {
                        items(hourlyWorkers, key = { it.id }) { hw ->
                            HourlyWorkerCard(
                                worker = hw,
                                currency = quinzaine.currency,
                                onEdit = { hourlyToEdit = hw },
                                onDelete = { onDeleteHourly(hw) }
                            )
                        }
                    }
                }

                2 -> {
                    // TAB 2: Fixed Posts
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Total des postes fixes : ${calculationResult.formattedFixedPosts}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandWarning
                                )
                                Text(
                                    text = "Charges fixes ou forfaitaires 100% personnalisables : Transport, primes, encadrement, matériel...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Postes fixes configurés",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = { isAddingFixedPost = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ajouter un poste fixe")
                            }
                        }
                    }

                    if (fixedPosts.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "Aucun poste fixe configuré. Ajoutez par exemple le transport d'ouvriers, une prime ou une charge d'encadrement.",
                                onAction = { isAddingFixedPost = true }
                            )
                        }
                    } else {
                        items(fixedPosts, key = { it.id }) { post ->
                            FixedPostCard(
                                post = post,
                                currency = quinzaine.currency,
                                normalRemainingDays = calculationResult.normalWorkDaysCount,
                                totalWorkers = calculationResult.totalWorkers,
                                onEdit = { fixedPostToEdit = post },
                                onDelete = { onDeleteFixedPost(post) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (isAddingGroup) {
        AddEditGroupDialog(
            onDismiss = { isAddingGroup = false },
            onConfirm = { name, count, paymentType, dailyRate, hourlyRate, hoursWorked, customAmount, description ->
                onAddGroup(name, count, paymentType, dailyRate, hourlyRate, hoursWorked, customAmount, description)
                isAddingGroup = false
            }
        )
    }

    groupToEdit?.let { group ->
        AddEditGroupDialog(
            initialGroup = group,
            onDismiss = { groupToEdit = null },
            onConfirm = { name, count, paymentType, dailyRate, hourlyRate, hoursWorked, customAmount, description ->
                onUpdateGroup(
                    group.copy(
                        name = name,
                        workerCount = count,
                        paymentTypeString = paymentType.name,
                        dailyRate = dailyRate,
                        hourlyRate = hourlyRate,
                        hoursWorked = hoursWorked,
                        customAmount = customAmount,
                        description = description
                    )
                )
                groupToEdit = null
            }
        )
    }

    if (isAddingHourly) {
        AddEditHourlyDialog(
            onDismiss = { isAddingHourly = false },
            onConfirm = { name, rate, hours, target, desc ->
                onAddHourly(name, rate, hours, target, desc)
                isAddingHourly = false
            }
        )
    }

    hourlyToEdit?.let { hw ->
        AddEditHourlyDialog(
            initialWorker = hw,
            onDismiss = { hourlyToEdit = null },
            onConfirm = { name, rate, hours, target, desc ->
                onUpdateHourly(hw.copy(name = name, hourlyRate = rate, hoursWorked = hours, targetAmount = target, description = desc))
                hourlyToEdit = null
            }
        )
    }

    if (isAddingFixedPost) {
        AddEditFixedPostDialog(
            onDismiss = { isAddingFixedPost = false },
            onConfirm = { name, cat, type, amt, qty, days, isLump ->
                onAddFixedPost(name, cat, type, amt, qty, days, isLump)
                isAddingFixedPost = false
            }
        )
    }

    fixedPostToEdit?.let { post ->
        AddEditFixedPostDialog(
            initialPost = post,
            onDismiss = { fixedPostToEdit = null },
            onConfirm = { name, cat, type, amt, qty, days, isLump ->
                onUpdateFixedPost(
                    post.copy(
                        name = name,
                        category = cat,
                        typeString = type.name,
                        amount = amt,
                        quantity = qty,
                        daysCount = days,
                        isDirectLumpSum = isLump
                    )
                )
                fixedPostToEdit = null
            }
        )
    }
}

@Composable
private fun WorkerGroupCard(
    group: WorkerGroupEntity,
    remainingDays: Int,
    currency: String,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val dailyTotal = group.dailySubtotal
    val remainingForecast = when (group.paymentType) {
        PaymentType.DAILY -> dailyTotal * remainingDays
        PaymentType.HOURLY -> dailyTotal * remainingDays
        PaymentType.CUSTOM_AMOUNT -> group.customAmount
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(BrandPrimary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = group.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = group.paymentType.labelFr,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandPrimary
                                )
                            }
                        }

                        val rateText = when (group.paymentType) {
                            PaymentType.DAILY -> "${group.workerCount} travailleurs × ${String.format("%.2f %s/j", group.dailyRate, currency)}"
                            PaymentType.HOURLY -> "${group.workerCount} travailleurs × ${String.format("%.2f %s/h (%.1f h/j)", group.hourlyRate, currency, group.hoursWorked)}"
                            PaymentType.CUSTOM_AMOUNT -> "${group.workerCount} travailleurs • Forfait ${String.format("%.2f %s", group.customAmount, currency)}"
                        }
                        Text(
                            text = rateText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Dupliquer", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = BrandError, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (group.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📌 ${group.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Coût journalier du groupe :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format("%.2f %s/j", dailyTotal, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Reste à prévoir ($remainingDays j) :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = String.format("%.2f %s", remainingForecast, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyWorkerCard(
    worker: HourlyWorkerEntity,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(BrandSecondary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = BrandSecondary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = worker.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${worker.hoursWorked} h × ${String.format("%.2f %s/h", worker.hourlyRate, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = BrandError, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (worker.targetAmount != null && worker.targetAmount > 0) {
                    Text(
                        text = "Cible: ${worker.targetAmount} $currency (${worker.hoursWorked} h)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "${worker.hoursWorked} heures prévues",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = String.format("%.2f %s", worker.subtotal, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandSecondary
                )
            }
        }
    }
}

@Composable
private fun FixedPostCard(
    post: FixedPostEntity,
    currency: String,
    normalRemainingDays: Int,
    totalWorkers: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val totalAmount = when (post.type) {
        FixedPostType.ONE_TIME, FixedPostType.CUSTOM_AMOUNT -> post.amount
        FixedPostType.PER_DAY -> post.amount * (if (post.daysCount > 0) post.daysCount else normalRemainingDays)
        FixedPostType.PER_WORKER -> post.amount * (if (post.quantity > 0) post.quantity else totalWorkers)
        FixedPostType.PER_DAY_PER_WORKER -> post.amount * (if (post.quantity > 0) post.quantity else totalWorkers) * (if (post.daysCount > 0) post.daysCount else normalRemainingDays)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(BrandWarning.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = BrandWarning, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = post.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${post.category} • ${post.type.labelFr}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = BrandError, modifier = Modifier.size(18.dp))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.type.shortDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.2f %s", totalAmount, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandWarning
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    message: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ajouter maintenant")
            }
        }
    }
}
