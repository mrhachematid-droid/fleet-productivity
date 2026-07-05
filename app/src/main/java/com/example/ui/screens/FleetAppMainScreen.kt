package com.example.ui.screens

import android.content.Context
import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.pdf.PdfExporter
import com.example.ui.theme.*
import com.example.ui.viewmodel.FleetViewModel
import com.example.ui.viewmodel.FleetViewModel.TrackerState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetAppMainScreen(viewModel: FleetViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    val drivers by viewModel.allDrivers.collectAsStateWithLifecycle()
    val trucks by viewModel.allTrucks.collectAsStateWithLifecycle()
    val machines by viewModel.allMachines.collectAsStateWithLifecycle()
    val shifts by viewModel.allShifts.collectAsStateWithLifecycle()
    val cycles by viewModel.allCycles.collectAsStateWithLifecycle()

    // Dialog flags
    var showAddManualCycleDialog by remember { mutableStateOf(false) }
    var showAddDriverDialog by remember { mutableStateOf(false) }
    var showAddTruckDialog by remember { mutableStateOf(false) }
    var showAddMachineDialog by remember { mutableStateOf(false) }

    // Filter current date cycles
    val dateCycles = remember(cycles, selectedDate) {
        cycles.filter { it.date == selectedDate }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BentoNavIndicator),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = BentoNavTextSelected,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Fleet Productivity",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BentoTextPrimary
                            )
                            Text(
                                "Shift A • 06:00 - 14:00",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = BentoTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.seedDemoData() },
                        modifier = Modifier.testTag("seed_data_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dataset,
                            contentDescription = "Load Sample Data",
                            tint = BentoCycleIconTint
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearAllCycles() },
                        modifier = Modifier.testTag("clear_data_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All Records",
                            tint = BentoQueueIconTint
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BentoNavBg,
                windowInsets = WindowInsets.navigationBars
            ) {
                val navigationItems = listOf(
                    Triple("Dashboard", Icons.Default.Dashboard, 0),
                    Triple("Timeline", Icons.AutoMirrored.Filled.ListAlt, 1),
                    Triple("Live Tracker", Icons.Default.Timer, 2),
                    Triple("Rankings", Icons.Default.Leaderboard, 3),
                    Triple("Shifts", Icons.Default.Compare, 4),
                    Triple("Export", Icons.Default.PictureAsPdf, 5)
                )

                navigationItems.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = currentTab == index,
                        onClick = { viewModel.setTab(index) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BentoNavTextSelected,
                            selectedTextColor = BentoNavTextSelected,
                            unselectedIconColor = BentoNavTextUnselected,
                            unselectedTextColor = BentoNavTextUnselected,
                            indicatorColor = BentoNavIndicator
                        ),
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentTab == 1) {
                FloatingActionButton(
                    onClick = { showAddManualCycleDialog = true },
                    containerColor = BentoCycleIconTint,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_manual_cycle_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Completed Cycle")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                },
                label = "TabContent"
            ) { targetTab ->
                when (targetTab) {
                    0 -> DashboardScreen(
                        cycles = dateCycles,
                        drivers = drivers,
                        onSeedTestData = { viewModel.seedDemoData() },
                        onNavigateToRankings = { viewModel.setTab(3) }
                    )
                    1 -> TimelineScreen(
                        cycles = dateCycles,
                        drivers = drivers,
                        machines = machines,
                        onDeleteCycle = { viewModel.deleteCycle(it) }
                    )
                    2 -> TrackerScreen(
                        viewModel = viewModel,
                        drivers = drivers,
                        trucks = trucks,
                        machines = machines,
                        shifts = shifts,
                        onAddDriver = { showAddDriverDialog = true },
                        onAddTruck = { showAddTruckDialog = true },
                        onAddMachine = { showAddMachineDialog = true }
                    )
                    3 -> RankingsScreen(
                        cycles = cycles,
                        drivers = drivers,
                        trucks = trucks,
                        machines = machines
                    )
                    4 -> ShiftComparisonScreen(
                        viewModel = viewModel,
                        cycles = cycles,
                        shifts = shifts
                    )
                    5 -> ExportScreen(
                        context = context,
                        cycles = cycles,
                        drivers = drivers,
                        trucks = trucks,
                        machines = machines,
                        shifts = shifts
                    )
                }
            }
        }
    }

    // Dynamic Dialog Popups
    if (showAddManualCycleDialog) {
        AddManualCycleDialog(
            drivers = drivers,
            trucks = trucks,
            machines = machines,
            shifts = shifts,
            selectedDate = selectedDate,
            onDismiss = { showAddManualCycleDialog = false },
            onConfirm = { t, d, m, s, tr, dt, q, l, trv, uq, u ->
                viewModel.logManualCycle(t, d, m, s, tr, dt, q, l, trv, uq, u)
                showAddManualCycleDialog = false
            }
        )
    }

    if (showAddDriverDialog) {
        AddEntityDialog(
            title = "Register Driver",
            label1 = "Driver ID (e.g. D05)",
            label2 = "Driver Name",
            onDismiss = { showAddDriverDialog = false },
            onConfirm = { id, name ->
                viewModel.addNewDriver(id, name)
                showAddDriverDialog = false
            }
        )
    }

    if (showAddTruckDialog) {
        AddEntityDialog(
            title = "Register Haul Truck",
            label1 = "Truck ID (e.g. T-05)",
            label2 = "Truck Model / Code",
            onDismiss = { showAddTruckDialog = false },
            onConfirm = { id, name ->
                viewModel.addNewTruck(id, name, 150.0)
                showAddTruckDialog = false
            }
        )
    }

    if (showAddMachineDialog) {
        AddEntityDialog(
            title = "Register Excavating Machine",
            label1 = "Machine ID (e.g. EX-103)",
            label2 = "Machine Description",
            onDismiss = { showAddMachineDialog = false },
            onConfirm = { id, name ->
                viewModel.addNewMachine(id, name, "Excavator")
                showAddMachineDialog = false
            }
        )
    }
}

// -----------------------------------------------------
// SCREEN 1: PRODUCTIVITY DASHBOARD
// -----------------------------------------------------
@Composable
fun DashboardScreen(
    cycles: List<TruckCycle>,
    drivers: List<Driver>,
    onSeedTestData: () -> Unit,
    onNavigateToRankings: () -> Unit = {}
) {
    if (cycles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    tint = BentoTextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Productivity Data Saved",
                    color = BentoTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Record cycles in the Live Tracker tab or tap below to seed sample fleet records.",
                    color = BentoTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSeedTestData,
                    colors = ButtonDefaults.buttonColors(containerColor = TealSuccess),
                    modifier = Modifier.testTag("seed_dashboard_button")
                ) {
                    Icon(Icons.Default.Dataset, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load Sample Fleet Scenario", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    // Mathematical Aggregators
    val totalLoads = cycles.size
    val avgCycle = cycles.map { it.cycleTimeMinutes }.average()
    
    var totalQload = 0.0
    var totalLoadTime = 0.0
    var totalTravel = 0.0
    var totalQunload = 0.0

    cycles.forEach {
        totalQload += (it.loadingStartMillis - it.queueStartMillis) / 60000.0
        totalLoadTime += (it.loadingEndMillis - it.loadingStartMillis) / 60000.0
        totalTravel += (it.unloadingQueueStartMillis - it.loadingEndMillis) / 60000.0
        totalQunload += (it.unloadMillis - it.unloadingQueueStartMillis) / 60000.0
    }

    val avgQload = totalQload / totalLoads
    val avgLoadTime = totalLoadTime / totalLoads
    val avgTravel = totalTravel / totalLoads
    val avgQunload = totalQunload / totalLoads

    // Active span hours calculation
    val minStart = cycles.minOf { it.queueStartMillis }
    val maxUnload = cycles.maxOf { it.unloadMillis }
    val workingHours = if (maxUnload > minStart) (maxUnload - minStart).toDouble() / 3600000.0 else 0.0
    val loadsPerHour = if (workingHours > 0) totalLoads / workingHours else 0.0

    val waitingTime = totalQload + totalQunload
    val totalCycleSum = cycles.sumOf { it.cycleTimeMinutes }
    val waitPercent = if (totalCycleSum > 0) (waitingTime / totalCycleSum) * 100.0 else 0.0
    val productivityScore = 100.0 - waitPercent

    // Top performers dynamic extraction
    val topPerformers = remember(cycles, drivers) {
        cycles.groupBy { it.driverId }
            .map { (driverId, driverCycles) ->
                val avgTime = driverCycles.map { it.cycleTimeMinutes }.average()
                val name = drivers.find { it.id == driverId }?.name ?: "Operator $driverId"
                val truck = driverCycles.firstOrNull()?.truckId ?: "Truck"
                // Score metric: 100 base, penalize excessive cycle times
                val score = (100.0 - (avgTime - 12.0).coerceAtLeast(0.0) * 1.8).coerceIn(60.0, 99.8)
                Triple(name, truck, score)
            }
            .sortedByDescending { it.third }
            .take(3)
    }

    // Shift comparison
    val shiftALoads = cycles.count { it.shiftId.contains("A", ignoreCase = true) || it.shiftId == "1" }
    val shiftBLoads = cycles.count { it.shiftId.contains("B", ignoreCase = true) || it.shiftId == "2" }
    val totalShiftLoads = (shiftALoads + shiftBLoads).coerceAtLeast(1)
    val shiftAPercent = (shiftALoads.toDouble() / totalShiftLoads * 100.0).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Primary Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Fleet Productivity",
                        color = BentoTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        "Live Fleet Metrics Overview",
                        color = BentoTextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Row of Primary Bento Grid Cards: Productivity & Loads
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Productivity score radial bar converted into standard horizontal Bento gauge
                Card(
                    modifier = Modifier.weight(1.5f),
                    colors = CardDefaults.cardColors(containerColor = BentoBlueCardBg),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PRODUCTIVITY SCORE",
                                color = BentoBlueCardText.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(BentoBlueCardText)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "+4.2%",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f", productivityScore),
                                color = BentoBlueCardText,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "%",
                                color = BentoBlueCardText.copy(alpha = 0.6f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        // Horizontal progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(BentoBlueCardText.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((productivityScore.toFloat() / 100f).coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(BentoBlueCardText)
                            )
                        }
                    }
                }

                // Metric: Total Loads Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = BentoPurpleCardBg),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "LOADS",
                            color = BentoPurpleCardText.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "$totalLoads",
                            color = BentoPurpleCardText,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Goal: 150",
                            color = BentoPurpleCardText.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Sub Metrics Row 1: Cycle Time & Loading Queue
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoSubMetricCard(
                    title = "CYCLE",
                    value = String.format(Locale.US, "%dm %ds", avgCycle.toInt(), ((avgCycle - avgCycle.toInt()) * 60).toInt()),
                    icon = Icons.Default.Timer,
                    iconBgColor = BentoCycleIconBg,
                    iconTintColor = BentoCycleIconTint,
                    modifier = Modifier.weight(1f)
                )
                BentoSubMetricCard(
                    title = "QUEUE",
                    value = String.format(Locale.US, "%dm %ds", avgQload.toInt(), ((avgQload - avgQload.toInt()) * 60).toInt()),
                    icon = Icons.Default.HourglassEmpty,
                    iconBgColor = BentoQueueIconBg,
                    iconTintColor = BentoQueueIconTint,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Sub Metrics Row 2: Production Rate & Active Operation Span
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoSubMetricCard(
                    title = "RATE",
                    value = String.format(Locale.US, "%.1f L/h", loadsPerHour),
                    icon = Icons.Default.Speed,
                    iconBgColor = Color(0x3D001D35),
                    iconTintColor = Color(0xFF001D35),
                    modifier = Modifier.weight(1f)
                )
                BentoSubMetricCard(
                    title = "ACTIVE SPAN",
                    value = String.format(Locale.US, "%.1f hrs", workingHours),
                    icon = Icons.Default.WorkOutline,
                    iconBgColor = Color(0x3D21005D),
                    iconTintColor = Color(0xFF21005D),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Driver Ranking Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOP PERFORMANCE",
                            color = BentoTextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        TextButton(
                            onClick = onNavigateToRankings,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                "View Rank",
                                color = BentoCycleIconTint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (topPerformers.isEmpty()) {
                        Text(
                            "No driver cycles completed yet.",
                            color = BentoTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    } else {
                        topPerformers.forEachIndexed { rankIndex, driverData ->
                            val rank = rankIndex + 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (rank == 1) BentoNavIndicator else Color(0xFFF3F3F3)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$rank",
                                            color = BentoTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${driverData.first} (${driverData.second})",
                                        color = BentoTextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", driverData.third),
                                    color = BentoTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Shift Comparison Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BentoCharcoalBg),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "SHIFT A EFFICIENCY",
                            color = BentoCharcoalText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "SHIFT B COMPARISON",
                            color = BentoCharcoalText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Dual progress indicator
                    val fraction = (shiftAPercent / 100f).coerceIn(0.1f, 0.9f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF49454F))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(BentoLavender)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (shiftALoads == 0 && shiftBLoads == 0) "85% Lead" else "$shiftAPercent% Loads",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (shiftALoads == 0 && shiftBLoads == 0) "-12% Prev" else "${100 - shiftAPercent}% Loads",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // CUSTOM BAR CHART: STAGE ANALYSIS (Refactored to Bento aesthetic)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "STAGE BOTTLENECK ANALYSIS",
                            color = BentoTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = BentoCycleIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val stages = listOf(
                        Quadruple("Loading Queue Time", avgQload, BentoQueueIconTint, "Waiting"),
                        Quadruple("Loading At Machine", avgLoadTime, BluePrimary, "Loading"),
                        Quadruple("Travel To Dump", avgTravel, TealSuccess, "Traveling"),
                        Quadruple("Unloading Queue", avgQunload, RedAlert, "Waiting")
                    )

                    stages.forEach { (label, duration, color, group) ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, color = BentoTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    String.format(Locale.US, "%.1f min", duration),
                                    color = color,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Simple bar representation
                            val maxAvg = stages.maxOf { it.second }.coerceAtLeast(1.0)
                            val barFraction = (duration / maxAvg).toFloat().coerceIn(0.01f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEEEEEE))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(barFraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BentoSubMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    color = BentoTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    color = BentoTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}


data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// -----------------------------------------------------
// SCREEN 2: DAILY TIMELINE
// -----------------------------------------------------
@Composable
fun TimelineScreen(
    cycles: List<TruckCycle>,
    drivers: List<Driver>,
    machines: List<Machine>,
    onDeleteCycle: (TruckCycle) -> Unit
) {
    if (cycles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = null,
                    tint = BentoTextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Daily Load Log is Empty",
                    color = BentoTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Press the Floating Action Button below to log manual cycles, or register and track cycles in the Live Tracker tab.",
                    color = BentoTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.US) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "CHRONOLOGICAL SHIFT LOG (${cycles.size} LOADS)",
                color = BentoTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }

        itemsIndexed(cycles) { index, cycle ->
            val dName = drivers.find { it.id == cycle.driverId }?.name ?: cycle.driverId
            val mName = machines.find { it.id == cycle.machineId }?.name ?: cycle.machineId
            val loadNum = cycles.size - index

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title block with load number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(BentoBlueCardBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    String.format(Locale.US, "%02d", loadNum),
                                    color = BentoBlueCardText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Load #${String.format(Locale.US, "%02d", loadNum)}",
                                color = BentoTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        IconButton(
                            onClick = { onDeleteCycle(cycle) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedAlert, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Drivers and machines involved
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TRUCK & DRIVER", color = BentoTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            Text("${cycle.truckId} ($dName)", color = BentoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("EXCAVATING MACHINE", color = BentoTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            Text(mName, color = BentoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Time sequence blocks
                    TimelineStageRow(time = timeFormatter.format(Date(cycle.queueStartMillis)), stageName = "Start Loading Queue", color = BentoQueueIconTint)
                    TimelineStageRow(time = timeFormatter.format(Date(cycle.loadingStartMillis)), stageName = "Start Loading", color = BluePrimary)
                    TimelineStageRow(time = timeFormatter.format(Date(cycle.loadingEndMillis)), stageName = "Finish Loading", color = Color(0xFF2E7D32))
                    TimelineStageRow(time = timeFormatter.format(Date(cycle.unloadingQueueStartMillis)), stageName = "Start Unloading Queue", color = RedAlert)
                    TimelineStageRow(time = timeFormatter.format(Date(cycle.unloadMillis)), stageName = "Unload", color = Color(0xFF2E7D32))

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cycle Time:", color = BentoTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(
                            String.format(Locale.US, "%.1f min", cycle.cycleTimeMinutes),
                            color = Color(0xFF2E7D32),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineStageRow(time: String, stageName: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            time,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = BentoTextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.width(60.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(stageName, color = BentoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Normal)
    }
}

// -----------------------------------------------------
// SCREEN 3: ACTIVE REAL-TIME TRACKER (SUPERVISOR TOOL)
// -----------------------------------------------------
@Composable
fun TrackerScreen(
    viewModel: FleetViewModel,
    drivers: List<Driver>,
    trucks: List<Truck>,
    machines: List<Machine>,
    shifts: List<Shift>,
    onAddDriver: () -> Unit,
    onAddTruck: () -> Unit,
    onAddMachine: () -> Unit
) {
    val state by viewModel.trackerState.collectAsStateWithLifecycle()
    
    val activeTruck by viewModel.activeTruckId.collectAsStateWithLifecycle()
    val activeDriver by viewModel.activeDriverId.collectAsStateWithLifecycle()
    val activeMachine by viewModel.activeMachineId.collectAsStateWithLifecycle()
    val activeShift by viewModel.activeShiftId.collectAsStateWithLifecycle()
    val activeTrailer by viewModel.activeTrailerId.collectAsStateWithLifecycle()

    val tQueueStart by viewModel.tQueueStart.collectAsStateWithLifecycle()
    val tLoadingStart by viewModel.tLoadingStart.collectAsStateWithLifecycle()
    val tLoadingEnd by viewModel.tLoadingEnd.collectAsStateWithLifecycle()
    val tUnloadingQueueStart by viewModel.tUnloadingQueueStart.collectAsStateWithLifecycle()

    var tickTime by remember { mutableStateOf(0L) }

    LaunchedEffect(state) {
        if (state != TrackerState.IDLE) {
            while (true) {
                tickTime = System.currentTimeMillis()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Live Dispatch Tracker",
            color = BentoTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

        if (state == TrackerState.IDLE) {
            // Configuration Selectors Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "CYCLE REGISTRATION SETUP",
                        color = BentoTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    
                    // Haul Truck Selector
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Select Active Haul Truck", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = onAddTruck, modifier = Modifier.height(32.dp)) { Text("+ Add New Truck", color = BentoCycleIconTint, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                        SelectorDropdown(
                            label = "Truck",
                            options = trucks.map { it.id },
                            selected = activeTruck,
                            onSelected = { viewModel.setActiveTruck(it) },
                            placeholder = "Choose Truck"
                        )
                    }

                    // Operator Selector
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Select Active Operator", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = onAddDriver, modifier = Modifier.height(32.dp)) { Text("+ Add New Driver", color = BentoCycleIconTint, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                        SelectorDropdown(
                            label = "Driver",
                            options = drivers.map { "${it.id} - ${it.name}" },
                            selected = drivers.find { it.id == activeDriver }?.let { "${it.id} - ${it.name}" } ?: "",
                            onSelected = { fullStr -> viewModel.setActiveDriver(fullStr.split(" - ").first()) },
                            placeholder = "Choose Driver"
                        )
                    }

                    // Loader Machine Selector
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Select Loading Machine / Shovel", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = onAddMachine, modifier = Modifier.height(32.dp)) { Text("+ Add New Machine", color = BentoCycleIconTint, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                        SelectorDropdown(
                            label = "Machine",
                            options = machines.map { "${it.id} - ${it.name}" },
                            selected = machines.find { it.id == activeMachine }?.let { "${it.id} - ${it.name}" } ?: "",
                            onSelected = { fullStr -> viewModel.setActiveMachine(fullStr.split(" - ").first()) },
                            placeholder = "Choose Machine"
                        )
                    }

                    // Active Work Shift
                    Column {
                        Text("Active Work Shift", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        SelectorDropdown(
                            label = "Shift",
                            options = shifts.map { "${it.id} - ${it.name}" },
                            selected = shifts.find { it.id == activeShift }?.let { "${it.id} - ${it.name}" } ?: "",
                            onSelected = { fullStr -> viewModel.setActiveShift(fullStr.split(" - ").first()) },
                            placeholder = "Choose Shift"
                        )
                    }

                    // Trailer designation
                    Column {
                        Text("Trailer Code", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = activeTrailer,
                            onValueChange = { viewModel.setActiveTrailer(it) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BentoCycleIconTint,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = BentoTextPrimary,
                                unfocusedTextColor = BentoTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("active_trailer_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val canStart = activeTruck.isNotEmpty() && activeDriver.isNotEmpty() && activeMachine.isNotEmpty() && activeShift.isNotEmpty()

                    Button(
                        onClick = { viewModel.startQueueLoad() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), disabledContainerColor = Color(0xFFE2E2E2)),
                        enabled = canStart,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("start_cycle_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (canStart) Color.White else Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "START LOADING QUEUE TIME",
                            color = if (canStart) Color.White else Color.Gray,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // Live Stopwatch Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "DISPATCH CYCLE STOPWATCH",
                        color = BentoTextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )

                    // Large formatted stopwatch display
                    val startTimer = when (state) {
                        TrackerState.QUEUING_LOAD -> tQueueStart
                        TrackerState.LOADING -> tLoadingStart
                        TrackerState.TRAVELING -> tLoadingEnd
                        TrackerState.QUEUING_UNLOAD -> tUnloadingQueueStart
                        else -> 0L
                    }

                    val elapsedMillis = if (tickTime > startTimer) tickTime - startTimer else 0L
                    val seconds = (elapsedMillis / 1000) % 60
                    val minutes = (elapsedMillis / 60000) % 60
                    val hours = (elapsedMillis / 3600000)

                    val timeStr = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

                    Text(
                        timeStr,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = BentoTextPrimary
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BentoBlueCardBg)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Active Stage: " + when (state) {
                                TrackerState.QUEUING_LOAD -> "Loading Queue Wait"
                                TrackerState.LOADING -> "At Machine / Shovel Loading"
                                TrackerState.TRAVELING -> "Traveling To Dump Site"
                                TrackerState.QUEUING_UNLOAD -> "At Dump Queue Wait"
                                else -> ""
                            },
                            color = BentoBlueCardText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // Dispatch metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("TRUCK ID", color = BentoTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(activeTruck, color = BentoTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("OPERATOR ID", color = BentoTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(activeDriver, color = BentoTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MACHINE", color = BentoTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(activeMachine, color = BentoTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Big Action Transition Button
                    when (state) {
                        TrackerState.QUEUING_LOAD -> {
                            Button(
                                onClick = { viewModel.startLoading() },
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("trigger_loading_button")
                            ) {
                                Icon(Icons.Default.MoveToInbox, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("TRUCK ARRIVED AT MACHINE / START LOADING", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        TrackerState.LOADING -> {
                            Button(
                                onClick = { viewModel.startTraveling() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("trigger_travel_button")
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("LOADING FINISHED / START TRAVEL TO DUMP", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        TrackerState.TRAVELING -> {
                            Button(
                                onClick = { viewModel.startQueueUnload() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD48A00)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("trigger_unload_queue_button")
                            ) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ARRIVED AT DUMP / START QUEUING", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        TrackerState.QUEUING_UNLOAD -> {
                            Button(
                                onClick = { viewModel.completeUnload() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("complete_unload_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("UNLOAD COMPLETED / SAVE RECORD", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {}
                    }

                    // Cancel active tracking
                    TextButton(
                        onClick = { viewModel.cancelActiveTracking() },
                        colors = ButtonDefaults.textButtonColors(contentColor = RedAlert),
                        modifier = Modifier.testTag("cancel_tracking_button")
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrogate & Discard Cycle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Dropdown Helper
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDropdown(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    placeholder: String
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected.ifEmpty { placeholder },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BentoCycleIconTint,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = BentoTextPrimary,
                unfocusedTextColor = if (selected.isEmpty()) BentoTextSecondary else BentoTextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .testTag("dropdown_selector_${label.lowercase()}")
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item, color = BentoTextPrimary, fontWeight = FontWeight.Medium) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    },
                    modifier = Modifier.testTag("dropdown_item_${item.replace(" ", "_")}")
                )
            }
        }
    }
}// -----------------------------------------------------
// SCREEN 4: RANKINGS & MACHINE ANALYSIS
// -----------------------------------------------------
@Composable
fun RankingsScreen(
    cycles: List<TruckCycle>,
    drivers: List<Driver>,
    trucks: List<Truck>,
    machines: List<Machine>
) {
    var subTab by remember { mutableStateOf(0) } // 0: Drivers, 1: Trucks, 2: Loader Machines

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Leaderboard & Fleet Rankings",
            color = BentoTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Sub tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val subTabs = listOf("Driver Rank", "Truck Rank", "Machine Stats")
            subTabs.forEachIndexed { index, title ->
                Button(
                    onClick = { subTab = index },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (subTab == index) BentoNavIndicator else MaterialTheme.colorScheme.surface,
                        contentColor = BentoTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("rank_subtab_$index"),
                    border = BorderStroke(1.dp, if (subTab == index) BentoCycleIconTint else MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (cycles.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No data to aggregate. Load scenarios to view leaderboard.", color = BentoTextSecondary, textAlign = TextAlign.Center)
            }
            return
        }

        Box(modifier = Modifier.weight(1f)) {
            when (subTab) {
                0 -> {
                    // Driver Rankings
                    val dRankings = cycles.groupBy { it.driverId }.map { (driverId, dCycles) ->
                        val dName = drivers.find { it.id == driverId }?.name ?: driverId
                        val loads = dCycles.size
                        val avgCycle = dCycles.map { it.cycleTimeMinutes }.average()
                        val waitingSum = dCycles.sumOf { 
                            ((it.loadingStartMillis - it.queueStartMillis) + (it.unloadMillis - it.unloadingQueueStartMillis)) / 60000.0
                        }
                        val cycleSum = dCycles.sumOf { it.cycleTimeMinutes }
                        val waitPercent = if (cycleSum > 0) (waitingSum / cycleSum) * 100.0 else 0.0
                        val score = 100.0 - waitPercent
                        DriverRankItem(dName, loads, avgCycle, waitingSum / loads, score)
                    }.sortedByDescending { it.score }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(dRankings) { index, item ->
                            RankingCard(rank = index + 1, name = item.name, loads = item.loads, subtitle = "Avg Cycle: " + String.format(Locale.US, "%.1f min", item.avgCycle), score = item.score, scoreLabel = "Prod Score")
                        }
                    }
                }
                1 -> {
                    // Truck Rankings
                    val tRankings = cycles.groupBy { it.truckId }.map { (truckId, tCycles) ->
                        val loads = tCycles.size
                        val avgCycle = tCycles.map { it.cycleTimeMinutes }.average()
                        val waitingSum = tCycles.sumOf { 
                            ((it.loadingStartMillis - it.queueStartMillis) + (it.unloadMillis - it.unloadingQueueStartMillis)) / 60000.0
                        }
                        val cycleSum = tCycles.sumOf { it.cycleTimeMinutes }
                        val waitPercent = if (cycleSum > 0) (waitingSum / cycleSum) * 100.0 else 0.0
                        val score = 100.0 - waitPercent
                        val workingHrs = cycleSum / 60.0
                        TruckRankItem(truckId, loads, avgCycle, workingHrs, score)
                    }.sortedByDescending { it.score }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(tRankings) { index, item ->
                            RankingCard(
                                rank = index + 1,
                                name = "Truck Code: ${item.id}",
                                loads = item.loads,
                                subtitle = "Usage: " + String.format(Locale.US, "%.1f hrs", item.workingHours) + " | Cycle: " + String.format(Locale.US, "%.1f m", item.avgCycle),
                                score = item.score,
                                scoreLabel = "Efficiency"
                            )
                        }
                    }
                }
                2 -> {
                    // Machine (Loader) Analysis
                    val mStats = cycles.groupBy { it.machineId }.map { (machineId, mCycles) ->
                        val mName = machines.find { it.id == machineId }?.name ?: machineId
                        val trucksServed = mCycles.map { it.truckId }.distinct().size
                        val avgLoadTime = mCycles.map { (it.loadingEndMillis - it.loadingStartMillis) / 60000.0 }.average()
                        val avgQueueTime = mCycles.map { (it.loadingStartMillis - it.queueStartMillis) / 60000.0 }.average()
                        val totalLoads = mCycles.size
                        MachineStatItem(mName, trucksServed, avgLoadTime, avgQueueTime, totalLoads)
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(mStats) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(item.name, color = BentoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Trucks Served", color = BentoTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text("${item.trucksServed}", color = BentoTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Avg Loading Time", color = BentoTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(String.format(Locale.US, "%.1f min", item.avgLoadTime), color = Color(0xFF0288D1), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Avg Queue Wait", color = BentoTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text(String.format(Locale.US, "%.1f min", item.avgQueueTime), color = Color(0xFFD48A00), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Total Loads", color = BentoTextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            Text("${item.totalLoads}", color = Color(0xFF2E7D32), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RankingCard(
    rank: Int,
    name: String,
    loads: Int,
    subtitle: String,
    score: Double,
    scoreLabel: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (rank) {
                                1 -> BentoNavIndicator
                                else -> Color(0xFFF3F3F3)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$rank",
                        color = BentoTextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(name, color = BentoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(subtitle, color = BentoTextSecondary, fontSize = 11.sp)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(scoreLabel, color = BentoTextSecondary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(
                    String.format(Locale.US, "%.1f%%", score),
                    color = Color(0xFF2E7D32),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text("$loads Loads", color = Color(0xFF0288D1), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

data class DriverRankItem(val name: String, val loads: Int, val avgCycle: Double, val avgWait: Double, val score: Double)
data class TruckRankItem(val id: String, val loads: Int, val avgCycle: Double, val workingHours: Double, val score: Double)
data class MachineStatItem(val name: String, val trucksServed: Int, val avgLoadTime: Double, val avgQueueTime: Double, val totalLoads: Int)

// -----------------------------------------------------
// SCREEN 5: SHIFT COMPARISON
// -----------------------------------------------------
@Composable
fun ShiftComparisonScreen(
    viewModel: FleetViewModel,
    cycles: List<TruckCycle>,
    shifts: List<Shift>
) {
    val compareA by viewModel.compareShiftA.collectAsStateWithLifecycle()
    val compareB by viewModel.compareShiftB.collectAsStateWithLifecycle()

    val cyclesA = remember(cycles, compareA) { cycles.filter { it.shiftId == compareA } }
    val cyclesB = remember(cycles, compareB) { cycles.filter { it.shiftId == compareB } }

    val nameA = shifts.find { it.id == compareA }?.name ?: compareA
    val nameB = shifts.find { it.id == compareB }?.name ?: compareB

    // Aggregators for Shift A
    val loadsA = cyclesA.size
    val avgCycleA = if (loadsA > 0) cyclesA.map { it.cycleTimeMinutes }.average() else 0.0
    val totalWaitA = if (loadsA > 0) cyclesA.sumOf { 
        ((it.loadingStartMillis - it.queueStartMillis) + (it.unloadMillis - it.unloadingQueueStartMillis)) / 60000.0
    } else 0.0
    val totalCycleSumA = cyclesA.sumOf { it.cycleTimeMinutes }
    val waitPercentA = if (totalCycleSumA > 0) (totalWaitA / totalCycleSumA) * 100.0 else 0.0
    val scoreA = if (loadsA > 0) 100.0 - waitPercentA else 0.0

    // Aggregators for Shift B
    val loadsB = cyclesB.size
    val avgCycleB = if (loadsB > 0) cyclesB.map { it.cycleTimeMinutes }.average() else 0.0
    val totalWaitB = if (loadsB > 0) cyclesB.sumOf { 
        ((it.loadingStartMillis - it.queueStartMillis) + (it.unloadMillis - it.unloadingQueueStartMillis)) / 60000.0
    } else 0.0
    val totalCycleSumB = cyclesB.sumOf { it.cycleTimeMinutes }
    val waitPercentB = if (totalCycleSumB > 0) (totalWaitB / totalCycleSumB) * 100.0 else 0.0
    val scoreB = if (loadsB > 0) 100.0 - waitPercentB else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Shift Comparative Analysis",
            color = BentoTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp
        )

        // Dropdown Selectors Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("First Shift", color = BentoTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectorDropdown(
                        label = "Shift A",
                        options = shifts.map { it.id },
                        selected = compareA,
                        onSelected = { viewModel.setCompareShiftA(it) },
                        placeholder = "Choose Shift A"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Second Shift", color = BentoTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    SelectorDropdown(
                        label = "Shift B",
                        options = shifts.map { it.id },
                        selected = compareB,
                        onSelected = { viewModel.setCompareShiftB(it) },
                        placeholder = "Choose Shift B"
                    )
                }
            }
        }

        // Head-to-Head Comparison Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("SHIFT METRIC COMPARISON", color = BentoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
                }

                // Row Headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1.2f))
                    Text(nameA, color = BluePrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Text(nameB, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // Comparison Rows
                ComparisonMetricRow(
                    label = "Total Loads",
                    valA = "$loadsA",
                    valB = "$loadsB",
                    isABetter = loadsA > loadsB,
                    isSame = loadsA == loadsB
                )

                ComparisonMetricRow(
                    label = "Productivity Score",
                    valA = String.format(Locale.US, "%.1f %%", scoreA),
                    valB = String.format(Locale.US, "%.1f %%", scoreB),
                    isABetter = scoreA > scoreB,
                    isSame = scoreA == scoreB
                )

                ComparisonMetricRow(
                    label = "Average Cycle Time",
                    valA = String.format(Locale.US, "%.1f min", avgCycleA),
                    valB = String.format(Locale.US, "%.1f min", avgCycleB),
                    isABetter = avgCycleA < avgCycleB && loadsA > 0, // Lower cycle time is better
                    isSame = avgCycleA == avgCycleB
                )

                ComparisonMetricRow(
                    label = "Waiting Delay Ratio",
                    valA = String.format(Locale.US, "%.1f %%", waitPercentA),
                    valB = String.format(Locale.US, "%.1f %%", waitPercentB),
                    isABetter = waitPercentA < waitPercentB && loadsA > 0, // Lower waiting percentage is better
                    isSame = waitPercentA == waitPercentB
                )
            }
        }
    }
}

@Composable
fun ComparisonMetricRow(
    label: String,
    valA: String,
    valB: String,
    isABetter: Boolean,
    isSame: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = BentoTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.2f))
        
        // Value A
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(valA, color = if (isSame) BentoTextPrimary else if (isABetter) Color(0xFF2E7D32) else BentoTextSecondary, fontWeight = if (isABetter) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
            if (!isSame && isABetter) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Check, contentDescription = "Better", tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
            }
        }

        // Value B
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(valB, color = if (isSame) BentoTextPrimary else if (!isABetter) Color(0xFF2E7D32) else BentoTextSecondary, fontWeight = if (!isABetter) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
            if (!isSame && !isABetter) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Check, contentDescription = "Better", tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
            }
        }
    }
}

// -----------------------------------------------------
// SCREEN 6: EXPORT PDF
// -----------------------------------------------------
@Composable
fun ExportScreen(
    context: Context,
    cycles: List<TruckCycle>,
    drivers: List<Driver>,
    trucks: List<Truck>,
    machines: List<Machine>,
    shifts: List<Shift>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = RedAlert,
                    modifier = Modifier.size(72.dp)
                )
                
                Text(
                    "EXPORT AUDITED FLEET REPORT",
                    color = BentoTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    "This action generates a comprehensive two-page corporate audit PDF report. It includes production metrics, stacked-stage bottleneck graphs, leaderboard rankings, chronological load tables, and a certification sign-off box.",
                    color = BentoTextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                Button(
                    onClick = {
                        PdfExporter.exportAndSharePdf(context, cycles, drivers, trucks, machines, shifts)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_pdf_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENERATE & EXPORT PDF REPORT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -----------------------------------------------------
// GENERAL DIALOGS
// -----------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualCycleDialog(
    drivers: List<Driver>,
    trucks: List<Truck>,
    machines: List<Machine>,
    shifts: List<Shift>,
    selectedDate: String,
    onDismiss: () -> Unit,
    onConfirm: (truckId: String, driverId: String, machineId: String, shiftId: String, trailerId: String, date: String, q: Double, l: Double, tr: Double, uq: Double, u: Double) -> Unit
) {
    var sTruck by remember { mutableStateOf(trucks.firstOrNull()?.id ?: "") }
    var sDriver by remember { mutableStateOf(drivers.firstOrNull()?.id ?: "") }
    var sMachine by remember { mutableStateOf(machines.firstOrNull()?.id ?: "") }
    var sShift by remember { mutableStateOf(shifts.firstOrNull()?.id ?: "") }
    var trailerCode by remember { mutableStateOf("T-01") }

    var qMin by remember { mutableStateOf("3.0") }
    var lMin by remember { mutableStateOf("4.5") }
    var tMin by remember { mutableStateOf("12.0") }
    var uqMin by remember { mutableStateOf("2.0") }
    var uMin by remember { mutableStateOf("1.5") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("LOG COMPLETED CYCLE", color = BentoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                SelectorDropdown(label = "Truck", options = trucks.map { it.id }, selected = sTruck, onSelected = { sTruck = it }, placeholder = "Select Truck")
                SelectorDropdown(label = "Driver", options = drivers.map { "${it.id} - ${it.name}" }, selected = drivers.find { it.id == sDriver }?.let { "${it.id} - ${it.name}" } ?: "", onSelected = { sDriver = it.split(" - ").first() }, placeholder = "Select Driver")
                SelectorDropdown(label = "Machine", options = machines.map { "${it.id} - ${it.name}" }, selected = machines.find { it.id == sMachine }?.let { "${it.id} - ${it.name}" } ?: "", onSelected = { sMachine = it.split(" - ").first() }, placeholder = "Select Machine")
                SelectorDropdown(label = "Shift", options = shifts.map { "${it.id} - ${it.name}" }, selected = shifts.find { it.id == sShift }?.let { "${it.id} - ${it.name}" } ?: "", onSelected = { sShift = it.split(" - ").first() }, placeholder = "Select Shift")

                OutlinedTextField(
                    value = trailerCode,
                    onValueChange = { trailerCode = it },
                    label = { Text("Trailer ID") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BentoTextPrimary,
                        unfocusedTextColor = BentoTextPrimary,
                        focusedBorderColor = BentoCycleIconTint,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = BentoCycleIconTint,
                        unfocusedLabelColor = BentoTextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                Text("Stage Durations (Minutes)", color = BentoTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qMin,
                        onValueChange = { qMin = it },
                        label = { Text("Queue Ld") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BentoTextPrimary,
                            unfocusedTextColor = BentoTextPrimary,
                            focusedBorderColor = BentoCycleIconTint,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = BentoCycleIconTint,
                            unfocusedLabelColor = BentoTextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lMin,
                        onValueChange = { lMin = it },
                        label = { Text("Loading") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BentoTextPrimary,
                            unfocusedTextColor = BentoTextPrimary,
                            focusedBorderColor = BentoCycleIconTint,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = BentoCycleIconTint,
                            unfocusedLabelColor = BentoTextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tMin,
                        onValueChange = { tMin = it },
                        label = { Text("Travel") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BentoTextPrimary,
                            unfocusedTextColor = BentoTextPrimary,
                            focusedBorderColor = BentoCycleIconTint,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = BentoCycleIconTint,
                            unfocusedLabelColor = BentoTextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uqMin,
                        onValueChange = { uqMin = it },
                        label = { Text("Queue Unl") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BentoTextPrimary,
                            unfocusedTextColor = BentoTextPrimary,
                            focusedBorderColor = BentoCycleIconTint,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = BentoCycleIconTint,
                            unfocusedLabelColor = BentoTextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uMin,
                        onValueChange = { uMin = it },
                        label = { Text("Unload") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BentoTextPrimary,
                            unfocusedTextColor = BentoTextPrimary,
                            focusedBorderColor = BentoCycleIconTint,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = BentoCycleIconTint,
                            unfocusedLabelColor = BentoTextSecondary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = RedAlert, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            onConfirm(
                                sTruck, sDriver, sMachine, sShift, trailerCode, selectedDate,
                                qMin.toDoubleOrNull() ?: 0.0,
                                lMin.toDoubleOrNull() ?: 0.0,
                                tMin.toDoubleOrNull() ?: 0.0,
                                uqMin.toDoubleOrNull() ?: 0.0,
                                uMin.toDoubleOrNull() ?: 0.0
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Log Cycle", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEntityDialog(
    title: String,
    label1: String,
    label2: String,
    onDismiss: () -> Unit,
    onConfirm: (id: String, name: String) -> Unit
) {
    var val1 by remember { mutableStateOf("") }
    var val2 by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(title, color = BentoTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                OutlinedTextField(
                    value = val1,
                    onValueChange = { val1 = it },
                    label = { Text(label1) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BentoTextPrimary,
                        unfocusedTextColor = BentoTextPrimary,
                        focusedBorderColor = BentoCycleIconTint,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = BentoCycleIconTint,
                        unfocusedLabelColor = BentoTextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = val2,
                    onValueChange = { val2 = it },
                    label = { Text(label2) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BentoTextPrimary,
                        unfocusedTextColor = BentoTextPrimary,
                        focusedBorderColor = BentoCycleIconTint,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = BentoCycleIconTint,
                        unfocusedLabelColor = BentoTextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = RedAlert, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (val1.isNotEmpty() && val2.isNotEmpty()) {
                                onConfirm(val1, val2)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = val1.isNotEmpty() && val2.isNotEmpty()
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
