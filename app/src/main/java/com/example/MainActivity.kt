package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CalculationBreakdownDialog
import com.example.ui.components.NewQuinzaineDialog
import com.example.ui.components.PdfExportDialog
import java.io.File
import com.example.ui.screens.BonsScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PrevisionScreen
import com.example.ui.screens.TransportScreen
import com.example.ui.screens.WorkersScreen
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.PrevisionPaieTheme
import com.example.ui.viewmodel.PayrollViewModel

sealed class BottomNavTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    object Accueil : BottomNavTab("accueil", "Accueil", Icons.Filled.Home, Icons.Outlined.Home, "nav_accueil")
    object Prevision : BottomNavTab("prevision", "Prévision", Icons.Filled.QueryStats, Icons.Outlined.QueryStats, "nav_prevision")
    object Calendrier : BottomNavTab("calendrier", "Calendrier", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_calendrier")
    object Paie : BottomNavTab("paie", "Paie", Icons.Filled.Payments, Icons.Outlined.Payments, "nav_paie")
    object Parametres : BottomNavTab("parametres", "Paramètres", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_parametres")
}

class MainActivity : ComponentActivity() {
    private val viewModel: PayrollViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PrevisionPaieTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PayrollViewModel) {
    val context = LocalContext.current
    var currentSection by rememberSaveable { mutableIntStateOf(0) }
    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var showBreakdownDialog by remember { mutableStateOf(false) }
    var showNewQuinzaineDialog by remember { mutableStateOf(false) }
    var showPdfDialog by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }

    val quinzaine by viewModel.activeQuinzaine.collectAsStateWithLifecycle()
    val allQuinzaines by viewModel.allQuinzaines.collectAsStateWithLifecycle()
    val workerGroups by viewModel.workerGroups.collectAsStateWithLifecycle()
    val hourlyWorkers by viewModel.hourlyWorkers.collectAsStateWithLifecycle()
    val fixedPosts by viewModel.fixedPosts.collectAsStateWithLifecycle()
    val calendarDays by viewModel.calendarDays.collectAsStateWithLifecycle()
    val calculationResult by viewModel.calculationResult.collectAsStateWithLifecycle()
    val activeId by viewModel.activeQuinzaineId.collectAsStateWithLifecycle()
    val transports by viewModel.transports.collectAsStateWithLifecycle()
    val bons by viewModel.bons.collectAsStateWithLifecycle()

    val tabs = listOf(
        BottomNavTab.Accueil,
        BottomNavTab.Prevision,
        BottomNavTab.Calendrier,
        BottomNavTab.Paie,
        BottomNavTab.Parametres
    )

    val handleExportPdf = {
        val file = viewModel.exportPrevisionToPdf(context)
        if (file != null) {
            generatedPdfFile = file
            showPdfDialog = true
        }
    }

    // Handle back button: if in Transport or Bons, return to Quinzaine; if in sub-tab, return to Accueil
    if (currentSection != 0) {
        BackHandler {
            currentSection = 0
        }
    } else if (currentTab != 0) {
        BackHandler {
            currentTab = 0
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Section 1: Quinzaine
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentSection = 0 }
                                .testTag("top_section_quinzaine"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentSection == 0) BrandPrimary else Color(0xFFF1F5F9)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🌾 Quinzaine",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (currentSection == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (currentSection == 0) Color.White else Color(0xFF475569)
                                )
                            }
                        }

                        // Section 2: Transport
                        Surface(
                            modifier = Modifier
                                .weight(1.15f)
                                .clickable { currentSection = 1 }
                                .testTag("top_section_transport"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentSection == 1) BrandSecondary else Color(0xFFF1F5F9)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🚍 Transport (${transports.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (currentSection == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (currentSection == 1) Color.White else Color(0xFF475569)
                                )
                            }
                        }

                        // Section 3: Bons
                        Surface(
                            modifier = Modifier
                                .weight(1.05f)
                                .clickable { currentSection = 2 }
                                .testTag("top_section_bons"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (currentSection == 2) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📄 Bons (${bons.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (currentSection == 2) FontWeight.Bold else FontWeight.Medium,
                                    color = if (currentSection == 2) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (currentSection == 0) {
                Surface(
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = currentTab == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = com.example.ui.theme.BrandCharcoal,
                                    selectedTextColor = com.example.ui.theme.BrandCharcoal,
                                    indicatorColor = Color(0xFFF1F5F9),
                                    unselectedIconColor = com.example.ui.theme.TextSecondary,
                                    unselectedTextColor = com.example.ui.theme.TextSecondary
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
        ) {
            when (currentSection) {
                0 -> {
                    when (currentTab) {
                        0 -> DashboardScreen(
                            quinzaine = quinzaine,
                            calculationResult = calculationResult,
                            workerGroups = workerGroups,
                            transports = transports,
                            bons = bons,
                            onOpenBreakdown = { showBreakdownDialog = true },
                            onShare = {
                                val text = viewModel.buildShareSummaryText()
                                viewModel.shareText(context, text, "Prévision Paie - ${quinzaine?.title ?: ""}")
                            },
                            onExportPdf = handleExportPdf,
                            onNewQuinzaine = { showNewQuinzaineDialog = true },
                            onLoadDemoScenario = { viewModel.loadOfficialDemoScenario() },
                            onNavigateToCalendar = { currentTab = 2 },
                            onNavigateToWorkers = { currentTab = 3 },
                            onNavigateToTransport = { currentSection = 1 },
                            onNavigateToBons = { currentSection = 2 }
                        )
                        1 -> PrevisionScreen(
                            quinzaine = quinzaine,
                            calculationResult = calculationResult,
                            onSaveDetails = { title, start, cur, end, amt, curr ->
                                viewModel.updateQuinzaineDetails(title, start, cur, end, amt, curr)
                            },
                            onOpenBreakdown = { showBreakdownDialog = true },
                            onShare = {
                                val text = viewModel.buildDetailedBreakdownText()
                                viewModel.shareText(context, text, "Détail Calcul Prévision Paie")
                            },
                            onExportPdf = handleExportPdf
                        )
                        2 -> CalendarScreen(
                            quinzaine = quinzaine,
                            calendarDays = calendarDays,
                            calculationResult = calculationResult,
                            onSetDayStatus = { date, status, note, holidayCustomAmount ->
                                viewModel.setDayStatus(date, status, note, holidayCustomAmount)
                            }
                        )
                        3 -> WorkersScreen(
                            quinzaine = quinzaine,
                            workerGroups = workerGroups,
                            hourlyWorkers = hourlyWorkers,
                            fixedPosts = fixedPosts,
                            calculationResult = calculationResult,
                            onAddGroup = { name, count, pType, dailyRate, hourlyRate, hours, customAmt, desc ->
                                viewModel.addWorkerGroup(name, count, pType, dailyRate, hourlyRate, hours, customAmt, desc)
                            },
                            onUpdateGroup = { group -> viewModel.updateWorkerGroup(group) },
                            onDeleteGroup = { group -> viewModel.deleteWorkerGroup(group) },
                            onDuplicateGroup = { group -> viewModel.duplicateWorkerGroup(group) },
                            onAddHourly = { name, rate, hours, target, desc ->
                                viewModel.addHourlyWorker(name, rate, hours, target, desc)
                            },
                            onUpdateHourly = { hw -> viewModel.updateHourlyWorker(hw) },
                            onDeleteHourly = { hw -> viewModel.deleteHourlyWorker(hw) },
                            onAddFixedPost = { name, cat, type, amt, qty, days, isLump ->
                                viewModel.addFixedPost(name, cat, type, amt, qty, days, isLump)
                            },
                            onUpdateFixedPost = { post -> viewModel.updateFixedPost(post) },
                            onDeleteFixedPost = { post -> viewModel.deleteFixedPost(post) }
                        )
                        4 -> HistoryScreen(
                            quinzaines = allQuinzaines,
                            activeQuinzaineId = activeId,
                            onSelectQuinzaine = { id ->
                                viewModel.selectQuinzaine(id)
                                currentTab = 0
                            },
                            onDeleteQuinzaine = { id -> viewModel.deleteCurrentQuinzaine(id) },
                            onDuplicateQuinzaine = { id -> viewModel.duplicateCurrentQuinzaine(id) },
                            onToggleCompleted = { viewModel.toggleQuinzaineStatus() },
                            onLoadDemoScenario = {
                                viewModel.loadOfficialDemoScenario()
                                currentTab = 0
                            },
                            onNewQuinzaine = { showNewQuinzaineDialog = true },
                            onShare = {
                                val text = viewModel.buildShareSummaryText()
                                viewModel.shareText(context, text, "Prévision Paie")
                            }
                        )
                    }
                }
                1 -> {
                    TransportScreen(
                        quinzaine = quinzaine,
                        transports = transports,
                        onAddTransport = { name, eff, price, days, note ->
                            viewModel.addTransport(name, eff, price, days, note)
                        },
                        onUpdateTransport = { t -> viewModel.updateTransport(t) },
                        onDeleteTransport = { t -> viewModel.deleteTransport(t) },
                        onDuplicateTransport = { t -> viewModel.duplicateTransport(t) },
                        onShare = {
                            val text = viewModel.buildShareTransportText()
                            viewModel.shareText(context, text, "Prévision Transport - ${quinzaine?.title ?: ""}")
                        }
                    )
                }
                2 -> {
                    BonsScreen(
                        quinzaine = quinzaine,
                        bons = bons,
                        onAddBon = { number, date, desc, qty, amt, note ->
                            viewModel.addBon(number, date, desc, qty, amt, note)
                        },
                        onUpdateBon = { b -> viewModel.updateBon(b) },
                        onDeleteBon = { b -> viewModel.deleteBon(b) },
                        onShare = {
                            val text = viewModel.buildShareBonsText()
                            viewModel.shareText(context, text, "Registre des Bons - ${quinzaine?.title ?: ""}")
                        }
                    )
                }
            }
        }
    }

    // Modal dialogs
    if (showBreakdownDialog && quinzaine != null) {
        CalculationBreakdownDialog(
            quinzaine = quinzaine!!,
            result = calculationResult,
            onDismiss = { showBreakdownDialog = false },
            onShare = {
                val text = viewModel.buildDetailedBreakdownText()
                viewModel.shareText(context, text, "Détail du calcul - ${quinzaine?.title ?: ""}")
            }
        )
    }

    if (showNewQuinzaineDialog) {
        NewQuinzaineDialog(
            onDismiss = { showNewQuinzaineDialog = false },
            onConfirm = { title, start, cur, end, amt, curr ->
                viewModel.createNewQuinzaine(title, start, cur, end, amt, curr)
                showNewQuinzaineDialog = false
                currentTab = 0
            }
        )
    }

    if (showPdfDialog && quinzaine != null) {
        PdfExportDialog(
            quinzaine = quinzaine!!,
            calculationResult = calculationResult,
            transports = transports,
            bons = bons,
            pdfFile = generatedPdfFile,
            onDismiss = { showPdfDialog = false },
            onSharePdf = {
                generatedPdfFile?.let { file ->
                    com.example.util.PrevisionPdfGenerator.sharePdf(
                        context,
                        file,
                        "Prévision Paie - ${quinzaine?.title ?: ""}"
                    )
                }
            },
            onViewPdf = {
                generatedPdfFile?.let { file ->
                    com.example.util.PrevisionPdfGenerator.viewPdf(context, file)
                }
            }
        )
    }
}
