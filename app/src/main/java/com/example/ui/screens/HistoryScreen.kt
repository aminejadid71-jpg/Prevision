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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuinzaineEntity
import com.example.ui.theme.BrandError
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.BrandSuccess
import com.example.util.DateHelper

@Composable
fun HistoryScreen(
    quinzaines: List<QuinzaineEntity>,
    activeQuinzaineId: Long?,
    onSelectQuinzaine: (Long) -> Unit,
    onDeleteQuinzaine: (Long) -> Unit,
    onDuplicateQuinzaine: (Long) -> Unit,
    onToggleCompleted: () -> Unit,
    onLoadDemoScenario: () -> Unit,
    onNewQuinzaine: () -> Unit,
    onShare: () -> Unit
) {
    var quinzaineToDelete by remember { mutableStateOf<QuinzaineEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📜 Historique & Paramètres",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary
                    )
                    Text(
                        text = "Consultez, dupliquez ou archivez vos quinzaines précédentes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onNewQuinzaine,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nouvelle")
                }
            }
        }

        // List of Quinzaines
        item {
            Text(
                text = "Quinzaines enregistrées localement (${quinzaines.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (quinzaines.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Aucune quinzaine sauvegardée.")
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onLoadDemoScenario) {
                            Text("Charger l'exemple de Septembre")
                        }
                    }
                }
            }
        } else {
            items(quinzaines, key = { it.id }) { q ->
                val isActive = q.id == activeQuinzaineId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectQuinzaine(q.id) }
                        .testTag("history_item_${q.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        if (isActive) 1.5.dp else 1.dp,
                        if (isActive) BrandSecondary else Color(0xFFE2E8F0)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 2.dp else 1.dp)
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
                                        .size(36.dp)
                                        .background(
                                            if (q.isCompleted) BrandSuccess.copy(alpha = 0.15f) else BrandPrimary.copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (q.isCompleted) Icons.Default.CheckCircle else Icons.Default.History,
                                        contentDescription = null,
                                        tint = if (q.isCompleted) BrandSuccess else BrandPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = q.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${DateHelper.formatFrenchShort(q.startDate)} → ${DateHelper.formatFrenchShort(q.endDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (q.isCompleted) BrandSuccess.copy(alpha = 0.15f) else BrandSecondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (q.isCompleted) "Terminée" else "En cours",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (q.isCompleted) BrandSuccess else BrandSecondary
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Montant arrêté au ${DateHelper.formatFrenchShort(q.currentDate)} :",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%,.2f %s", q.currentAmount, q.currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary
                                )
                            }

                            Row {
                                if (isActive) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BrandPrimary,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    ) {
                                        Text(
                                            text = "Actif",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onSelectQuinzaine(q.id) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Ouvrir", fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                IconButton(onClick = { onDuplicateQuinzaine(q.id) }, modifier = Modifier.size(34.dp)) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Dupliquer", modifier = Modifier.size(16.dp))
                                }

                                IconButton(onClick = { quinzaineToDelete = q }, modifier = Modifier.size(34.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Supprimer", tint = BrandError, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Paramètres & Fonctionnalités
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = BrandPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Paramètres de l'application", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(thickness = 0.5.dp)

                    SettingRow(
                        title = "Devise par défaut",
                        description = "Dirham Marocain (DH / MAD)",
                        actionText = "DH"
                    )

                    SettingRow(
                        title = "Langue de l'interface",
                        description = "Français (Arabe et Darija prêts pour extension)",
                        actionText = "Français"
                    )

                    SettingRow(
                        title = "Règle de majoration jour férié",
                        description = "Payé normalement (1x) ou Payé double (2x)",
                        actionText = "Personnalisable"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Statut de la quinzaine active", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Marquer comme clôturée / payée", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = quinzaines.find { it.id == activeQuinzaineId }?.isCompleted ?: false,
                            onCheckedChange = { onToggleCompleted() }
                        )
                    }

                    OutlinedButton(
                        onClick = onLoadDemoScenario,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Recharger le scénario test (30 ouvriers / 46,779.97 DH)")
                    }
                }
            }
        }

        // Section: Confidentialité & Hors-ligne
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(BrandSuccess.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = BrandSuccess, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "100% Hors-ligne & Sécurisé",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toutes les données et calculs restent strictement sur votre téléphone. Aucun compte ni connexion Internet n'est requis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Delete confirmation dialog
    quinzaineToDelete?.let { q ->
        AlertDialog(
            onDismissRequest = { quinzaineToDelete = null },
            title = { Text("Supprimer cette quinzaine ?") },
            text = { Text("Voulez-vous supprimer définitivement la quinzaine '${q.title}' et tout son historique ?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteQuinzaine(q.id)
                        quinzaineToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandError)
                ) {
                    Text("Supprimer")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { quinzaineToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    actionText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = actionText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary
            )
        }
    }
}
