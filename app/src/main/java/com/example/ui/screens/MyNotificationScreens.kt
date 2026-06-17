package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.viewmodel.MyNotificationViewModel
import com.example.ui.viewmodel.SimulationAlert
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyNotificationAppView(viewModel: MyNotificationViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val masterBlockEnabled by viewModel.masterBlockEnabled.collectAsStateWithLifecycle()
    val activeFocus by viewModel.activeFocusMode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val simulationAlert = viewModel.simulationResult.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.NotificationsActive,
                                    contentDescription = "App Icon Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "MyNotification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (masterBlockEnabled) Color(0xFF4CAF50) else Color(0xFFFF9800))
                                )
                                Text(
                                    text = if (masterBlockEnabled) "Master Blocking ON" else "Standard Intercept Mode",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    FilledTonalIconToggleButton(
                        checked = masterBlockEnabled,
                        onCheckedChange = { viewModel.setMasterBlockEnabled(it) },
                        modifier = Modifier.testTag("master_block_toggle")
                    ) {
                        Icon(
                            if (masterBlockEnabled) Icons.Filled.Shield else Icons.Outlined.Shield,
                            contentDescription = "Master Shield switch"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                listOf(
                    NavigationItem("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
                    NavigationItem("inbox", "Vault", Icons.Filled.Inbox, Icons.Outlined.Inbox, "tab_inbox"),
                    NavigationItem("focus", "Focus Modes", Icons.Filled.Timer, Icons.Outlined.Timer, "tab_focus"),
                    NavigationItem("apps", "App Blocker", Icons.Filled.Apps, Icons.Outlined.Apps, "tab_apps"),
                    NavigationItem("filters", "Smart Shield", Icons.Filled.Tune, Icons.Outlined.Tune, "tab_filters")
                ).forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item.id,
                        onClick = { viewModel.setSelectedTab(item.id) },
                        icon = {
                            Icon(
                                if (selectedTab == item.id) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label, fontSize = 11.sp) },
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                "dashboard" -> DashboardScreen(viewModel)
                "inbox" -> InboxScreen(viewModel)
                "focus" -> FocusScreen(viewModel)
                "apps" -> AppsScreen(viewModel)
                "filters" -> FilteringScreen(viewModel)
                else -> DashboardScreen(viewModel)
            }

            // SIMULATED NOTIFICATION OVERLAY POPUP
            if (simulationAlert != null) {
                SimulationAlertDialog(
                    alert = simulationAlert,
                    onDismiss = { viewModel.clearSimulationResult() }
                )
            }
        }
    }
}

private data class NavigationItem(
    val id: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)

// ==========================================
// 1. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(viewModel: MyNotificationViewModel) {
    val allSaved by viewModel.allSavedNotifications.collectAsStateWithLifecycle()
    val allApps by viewModel.allBlockedApps.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    val activeFocus by viewModel.activeFocusMode.collectAsStateWithLifecycle()
    val masterBlockEnabled by viewModel.masterBlockEnabled.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Status Master Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                "Intelligent Shield",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (masterBlockEnabled) "Strict Isolation Active" else "Smart Intercept Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Icon(
                            if (masterBlockEnabled) Icons.Filled.Security else Icons.Filled.SafetyCheck,
                            contentDescription = "Shield State",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Total Intercepted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "${allSaved.size} Alerts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Focus Streak",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "3 Days",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    if (activeFocus != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Timer,
                                    contentDescription = "Focus Timer",
                                    tint = Color(activeFocus!!.colorHex.toColorIntOrNull() ?: 0xFF4CAF50.toInt())
                                )
                                Text(
                                    text = "Active Session: ${activeFocus!!.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    "TAP TO OFF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { viewModel.toggleFocusMode(activeFocus!!) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // STATS DRAWINGS SECTION
        item {
            Text(
                "Activity Analytics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Interceptions by Channel Group",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Custom Compose Canvas Chart!
                    WeeklyBlockedChart(allSaved)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItem("Social", Color(0xFF3F51B5))
                        LegendItem("Promo", Color(0xFFE91E63))
                        LegendItem("Work", Color(0xFF4CAF50))
                        LegendItem("OTP", Color(0xFFFF9800))
                    }
                }
            }
        }

        // HEATP MAP HOUR GRID
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Hourly Heatmap (24-Hour Intercepts)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    ActivityHeatmap(allSaved)

                    Text(
                        "Intensity reflects relative frequency per hour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // QUICK NOTIFICATION DISPATCH (TEST SUITE SUITE)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Simulate Notification Intercept",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Go to Smart Shield to post customized debug alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { viewModel.setSelectedTab("filters") },
                        modifier = Modifier.testTag("simulate_from_dashboard")
                    ) {
                        Text("Simulate")
                    }
                }
            }
        }

        // RECENT AUDIT LOG ROW HEADERS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Activity Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Clear Logs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable { viewModel.clearLogs() }
                        .testTag("clear_logs_button")
                )
            }
        }

        if (recentLogs.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier.padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No activity logged yet. Fire a simulated alert!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recentLogs) { log ->
                AuditLogItemView(log)
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// -------------------------------------------------------------
// COMPOSABLE VISUAL CHARTS (CANVAS BASED FOR PERFECT OUTCOME)
// -------------------------------------------------------------
@Composable
fun WeeklyBlockedChart(notifications: List<SavedNotificationEntity>) {
    // Count categorization ratios
    val socialCount = notifications.count { it.category == "Social" }.toFloat()
    val promoCount = notifications.count { it.category == "Promo" }.toFloat()
    val workCount = notifications.count { it.category == "Work" }.toFloat()
    val otpCount = notifications.count { it.category == "OTP" }.toFloat()
    val generalCount = notifications.count { it.category == "General" }.toFloat()

    val total = socialCount + promoCount + workCount + otpCount + generalCount

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val width = size.width
        val height = size.height

        // Background lines
        val lineCount = 4
        for (i in 0..lineCount) {
            val y = height * (i.toFloat() / lineCount)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        if (total == 0f) {
            // Empty state baseline - draw simulated charts with dash overlays
            val sampleCounts = listOf(45f, 75f, 30f, 90f, 50f)
            val barWidth = width / 12f
            val spacing = (width - (barWidth * 5)) / 6f
            for (index in 0 until 5) {
                val valH = sampleCounts[index]
                val left = spacing + index * (barWidth + spacing)
                val top = height - valH

                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.15f),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, valH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }
            return@Canvas
        }

        // Draw proportional bar graph
        val categories = listOf(
            Triple("Social", socialCount, Color(0xFF3F51B5)),
            Triple("Promo", promoCount, Color(0xFFE91E63)),
            Triple("Work", workCount, Color(0xFF4CAF50)),
            Triple("OTP", otpCount, Color(0xFFFF9800)),
            Triple("General", generalCount, Color(0xFF00bcd4))
        )

        val barWidth = width / 10f
        val numBars = categories.size
        val spacing = (width - (barWidth * numBars)) / (numBars + 1)

        // Find max value to scale chart proportionally
        val maxVal = categories.maxOf { it.second }.coerceAtLeast(1f)

        categories.forEachIndexed { index, (label, count, color) ->
            if (count > 0f) {
                val fraction = count / maxVal
                val barHeight = (height - 20.dp.toPx()) * fraction
                val left = spacing + index * (barWidth + spacing)
                val top = height - barHeight - 10.dp.toPx()

                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight.coerceAtLeast(4.dp.toPx())),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun ActivityHeatmap(notifications: List<SavedNotificationEntity>) {
    // Generate simulated hour maps if empty, otherwise count actual ones
    val hourCounts = remember(notifications) {
        val map = IntArray(24) { 0 }
        notifications.forEach { item ->
            val hour = GregorianCalendar().apply { timeInMillis = item.receivedAt }.get(Calendar.HOUR_OF_DAY)
            if (hour in 0..23) {
                map[hour]++
            }
        }
        // If empty, prefill some pretty gradient points
        if (map.all { it == 0 }) {
            map[9] = 5
            map[10] = 3
            map[12] = 8
            map[14] = 4
            map[18] = 12
            map[20] = 7
            map[21] = 9
            map[22] = 15
        }
        map
    }

    val maxCount = hourCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val baseColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (i in 0..23) {
            val count = hourCounts[i]
            val opacity = (count.toFloat() / maxCount).coerceAtLeast(0.08f)
            val hoverTitle = "$i:00"

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(baseColor.copy(alpha = opacity)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "$i",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (opacity > 0.6f) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AuditLogItemView(log: NotificationLogEntity) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = formatter.format(Date(log.timestamp))

    Surface(
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action Icon Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (log.action.startsWith("BLOCKED")) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (log.action.startsWith("BLOCKED")) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = "Intercept Action status",
                    tint = if (log.action.startsWith("BLOCKED")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        log.appLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            log.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    log.title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${log.action} • $formattedTime",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// 2. INBOX SCREEN (THE VAULT)
// ==========================================
@Composable
fun InboxScreen(viewModel: MyNotificationViewModel) {
    val searchResults by viewModel.filteredSavedNotifications.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchTerm.collectAsStateWithLifecycle()
    val activeCategory by viewModel.searchCategory.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vault Main Headline Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Secure Vault Inbox",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${searchResults.size} blocked alerts encrypted locally",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { viewModel.clearAllNotifications() },
                modifier = Modifier.testTag("delete_all_inbox")
            ) {
                Icon(
                    Icons.Filled.DeleteSweep,
                    contentDescription = "Swipe clean all inbox data",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Search Bar Block
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchTerm(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vault_search"),
            placeholder = { Text("Search title, body content, package...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchTerm("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear query")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Horizontal Categorized Tags
        val categories = listOf("All", "Social", "Promo", "Work", "OTP", "General")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val selected = activeCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setSearchCategory(category) },
                    label = { Text(category) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = "selected", modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.testTag("chip_category_$category")
                )
            }
        }

        // List Scroll Layout
        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Outlined.FolderZip,
                        contentDescription = "Empty Vault",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(72.dp)
                    )
                    Text(
                        "Vault state is pristine",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "No blocked notifications in this category.\nConfigure app restrictions or run simulated events!",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(searchResults, key = { it.id }) { item ->
                    VaultItemRow(
                        item = item,
                        onDelete = { viewModel.deleteNotification(item.id) },
                        onTogglePin = { viewModel.toggleNotificationPin(item) },
                        onCopyOtp = { otpText ->
                            clipboardManager.setText(AnnotatedString(otpText))
                            Toast.makeText(context, "OTP Mobile verification code copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VaultItemRow(
    item: SavedNotificationEntity,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onCopyOtp: (String) -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val formattedTime = formatter.format(Date(item.receivedAt))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isPinned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_item_${item.id}")
            .border(
                width = if (item.isPinned) 1.dp else 0.dp,
                color = if (item.isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: App info and Pinned/Trash actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Initial Letter Avatar
                val initialLetter = if (item.appLabel.isNotEmpty()) item.appLabel.take(1) else "?"
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (item.category) {
                                "Social" -> Color(0xFF3F51B5).copy(alpha = 0.15f)
                                "Promo" -> Color(0xFFE91E63).copy(alpha = 0.15f)
                                "Work" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                "OTP" -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                else -> Color(0xFF00BCD4).copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initialLetter,
                        fontWeight = FontWeight.Bold,
                        color = when (item.category) {
                            "Social" -> Color(0xFF3F51B5)
                            "Promo" -> Color(0xFFE91E63)
                            "Work" -> Color(0xFF4CAF50)
                            "OTP" -> Color(0xFFFF9800)
                            else -> Color(0xFF00BCD4)
                        },
                        fontSize = 14.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            item.appLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                item.category,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Text(
                        formattedTime,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Actions: Pin and Delete
                IconButton(onClick = onTogglePin, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin notification",
                        tint = if (item.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Swipe remove item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Msg Details
            Text(
                item.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                item.body,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // OTP SPECIFIC DISPLAY EXTRAS
            if (item.isOtp && item.otpCode != null) {
                Surface(
                    color = Color(0xFFFF9800).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.VpnKey, contentDescription = "OTP code key", tint = Color(0xFFFF9800))
                            Text(
                                "Verification OTP Code: ",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                item.otpCode,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF9800),
                                letterSpacing = 2.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Button(
                            onClick = { onCopyOtp(item.otpCode) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("copy_otp_${item.id}")
                        ) {
                            Text("Copy", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. FOCUS MODES SCREEN
// ==========================================
@Composable
fun FocusScreen(viewModel: MyNotificationViewModel) {
    val focusModes by viewModel.allFocusModes.collectAsStateWithLifecycle()
    val activeFocus by viewModel.activeFocusMode.collectAsStateWithLifecycle()
    val schedules by viewModel.allSchedules.collectAsStateWithLifecycle()

    var showCreatePopup by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Headline Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Quiet Focus Modes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (activeFocus != null) "Strict Block - '${activeFocus!!.name}' Running" else "Select a focus preset to block notifications",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (activeFocus != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = { showCreatePopup = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_focus_mode_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "add icon", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Customize")
            }
        }

        // Active Banner Detail if any is running
        AnimatedVisibility(
            visible = activeFocus != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (activeFocus != null) {
                Surface(
                    color = Color(activeFocus!!.colorHex.toColorIntOrNull() ?: 0xFF4CAF50.toInt()).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = Color(activeFocus!!.colorHex.toColorIntOrNull() ?: 0xFF4CAF50.toInt()).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(activeFocus!!.colorHex.toColorIntOrNull() ?: 0xFF4CAF50.toInt())),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                getVectorIcon(activeFocus!!.iconName),
                                contentDescription = "Active icon",
                                tint = Color.White
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Locked in Focus: ${activeFocus!!.name}",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Interceptions will bypass ringtone/popups entirely and silently land in Vault.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { viewModel.toggleFocusMode(activeFocus!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Stop", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Grid preset lists
        Text(
            "Focus Mode Presets",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(focusModes) { mode ->
                val isActiveMode = mode.isActive
                val modeColor = Color(mode.colorHex.toColorIntOrNull() ?: 0xFF3F51B5.toInt())

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("focus_mode_card_${mode.id}")
                        .clickable { viewModel.toggleFocusMode(mode) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActiveMode) modeColor.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    border = BorderStroke(
                        width = if (isActiveMode) 2.dp else 1.dp,
                        color = if (isActiveMode) modeColor else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isActiveMode) modeColor else modeColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    getVectorIcon(mode.iconName),
                                    contentDescription = "mode icon",
                                    tint = if (isActiveMode) Color.White else modeColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (isActiveMode) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(modeColor)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "LIVE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                mode.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (isActiveMode) "Session Active" else "Idle State",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Allowed details
                        val allowedAppsCount = if (mode.allowedPackages.isEmpty()) 0 else mode.allowedPackages.split(",").size
                        val blockedAppsCount = if (mode.blockedPackages.isEmpty()) 0 else mode.blockedPackages.split(",").size

                        Text(
                            text = "${allowedAppsCount} Alwd • ${blockedAppsCount} Blkd",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // TIME SCHEDULE TRIGGER BULLET SECTION
        Text(
            "Quiet Quiet-Hours Schedules",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (schedules.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "No automatic schedules active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Configure silent hours under 'Smart Shield' section.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(schedules) { schedule ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Schedule, contentDescription = "Active schedule", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(
                                "${schedule.name}: ${formatTime(schedule.startHour, schedule.startMinute)} - ${formatTime(schedule.endHour, schedule.endMinute)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { viewModel.deleteSchedule(schedule) }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "clear schedule", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // CREATE FOCUS MODE DIALOG POPUP
    if (showCreatePopup) {
        CreateFocusModeDialog(
            allAppsList = listOf("Instagram", "WhatsApp", "Slack", "Gmail", "LinkedIn", "YouTube", "Telecom"),
            onSubmit = { name, icon, color, allowed, blocked ->
                // Simulate package names
                val mockMap = mapOf(
                    "Instagram" to "com.instagram.android",
                    "WhatsApp" to "com.whatsapp",
                    "Slack" to "com.slack",
                    "Gmail" to "com.google.android.gm",
                    "LinkedIn" to "com.linkedin.android",
                    "YouTube" to "com.google.android.youtube",
                    "Telecom" to "com.android.server.telecom"
                )
                viewModel.createFocusMode(
                    name = name,
                    icon = icon,
                    colorHex = color,
                    allowedApps = allowed.mapNotNull { mockMap[it] },
                    blockedApps = blocked.mapNotNull { mockMap[it] }
                )
                showCreatePopup = false
            },
            onDismiss = { showCreatePopup = false }
        )
    }
}

private fun String.toColorIntOrNull(): Int? {
    return try {
        android.graphics.Color.parseColor(this)
    } catch (_: Exception) {
        null
    }
}

private fun formatTime(hour: Int, min: Int): String {
    return String.format("%02d:%02d", hour, min)
}

private fun getVectorIcon(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (name) {
        "school" -> Icons.Filled.School
        "bedtime" -> Icons.Filled.Bedtime
        "work" -> Icons.Filled.Work
        "church" -> Icons.Filled.Church
        "description" -> Icons.Filled.Description
        "sports_esports" -> Icons.Filled.SportsEsports
        "flight" -> Icons.Filled.Flight
        "directions_car" -> Icons.Filled.DirectionsCar
        "music_note" -> Icons.Filled.MusicNote
        else -> Icons.Filled.Timer
    }
}

// ==========================================
// 4. APP BLOCKER/REGISTRY SCREEN
// ==========================================
@Composable
fun AppsScreen(viewModel: MyNotificationViewModel) {
    val appsList by viewModel.allBlockedApps.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedTabFilter by remember { mutableStateOf("All") } // All, Blocked, Allowed

    val filteredApps = appsList.filter { app ->
        val matchesQuery = app.appLabel.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedTabFilter) {
            "Blocked" -> app.isBlocked
            "Allowed" -> !app.isBlocked
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header
        Column {
            Text(
                "App-Specific Interceptions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Configure default block states, priority levels, and rate limits per application",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Search app text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter applications by name...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_blocker_search"),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Filter chips: All / Blocked / Allowed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Blocked", "Allowed").forEach { type ->
                FilterChip(
                    selected = selectedTabFilter == type,
                    onClick = { selectedTabFilter = type },
                    label = { Text(type) },
                    modifier = Modifier.testTag("app_filter_chip_$type")
                )
            }
        }

        // Apps Lazy Column scroll
        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No applications match the search criteria",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppRegistryConfigRow(
                        app = app,
                        onToggleBlock = { viewModel.toggleAppBlock(app) },
                        onPriorityChange = { level -> viewModel.setAppPriority(app, level) },
                        onLimitChange = { limit -> viewModel.setAppRateLimit(app, limit) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppRegistryConfigRow(
    app: BlockedAppEntity,
    onToggleBlock: () -> Unit,
    onPriorityChange: (String) -> Unit,
    onLimitChange: (Int) -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_row_${app.packageName}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header App Avatar and Block switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Initial Circle logo
                val initial = if (app.appLabel.isNotEmpty()) app.appLabel.take(1) else "?"
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initial,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        app.appLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        app.packageName,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Block Switch
                Switch(
                    checked = app.isBlocked,
                    onCheckedChange = { onToggleBlock() },
                    modifier = Modifier.testTag("app_block_switch_${app.packageName}")
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Extra Settings: Intercept Priority dropdown and Rate Limit Text Fields
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Dropdown priority
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        onClick = { expandedDropdown = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lvl: ${app.priority}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "dropdown arrow", modifier = Modifier.size(14.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        listOf("Critical", "High", "Normal", "Low", "Silent").forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level, fontSize = 12.sp) },
                                onClick = {
                                    onPriorityChange(level)
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // Rate Limit Integer trigger
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .clickable {
                            val nextLimit = when (app.maxPerHour) {
                                0 -> 5
                                5 -> 15
                                15 -> 30
                                else -> 0
                            }
                            onLimitChange(nextLimit)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (app.maxPerHour == 0) "Limit: None" else "Limit: ${app.maxPerHour}/hr",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Status Details Badge Indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (app.isBlocked) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (app.isBlocked) "MUTED" else "ACTIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (app.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ==========================================
// 5. SMART SHIELD / CONFIG SCREEN
// ==========================================
@Composable
fun FilteringScreen(viewModel: MyNotificationViewModel) {
    val keywords by viewModel.allKeywords.collectAsStateWithLifecycle()
    val schedules by viewModel.allSchedules.collectAsStateWithLifecycle()
    val focusModes by viewModel.allFocusModes.collectAsStateWithLifecycle()

    var textQueryKeyword by remember { mutableStateOf("") }
    var activeSchedulePresetName by remember { mutableStateOf("") }
    var scheduleStartHour by remember { mutableStateOf("") }
    var scheduleStartMinute by remember { mutableStateOf("") }
    var scheduleEndHour by remember { mutableStateOf("") }
    var scheduleEndMinute by remember { mutableStateOf("") }
    var selectedLinkedFocusModeId by remember { mutableStateOf<Long?>(null) }

    // Simulator input states
    var simAppLabel by remember { mutableStateOf("Instagram") }
    var simTitle by remember { mutableStateOf("sarah_k") }
    var simBody by remember { mutableStateOf("Are we still meeting for coffee at 5? ☕") }
    var simChannel by remember { mutableStateOf("direct") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Header Section
        item {
            Column {
                Text(
                    "Smart Shield Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Configure spam filters, schedules, and test the real-time simulation engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // SECTION A: SOCIAL MEDIA SHIELD & KEYWORDS Spams
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Social Media Isolation Shield",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Mutes Facebook, Instagram, Twitter/X, Snapchat in 1 tap",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Master Shield Quick action
                    Button(
                        onClick = {
                            viewModel.simulateNotification(
                                "com.instagram.android", "Instagram", "Social Media Shield",
                                "All social media packages have been restricted globally in local database.", "posts"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify Social Shield Restriction Logs")
                    }
                }
            }
        }

        // SECTION B: SPAM KEYWORDS DEFINITIONS
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Anti-Spam Keyword Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Delete or intercept any alerts containing these exact phrases (e.g. 'discount'):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Keyword list
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(keywords) { kw ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(30.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(kw.keyword, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "clear keyword",
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { viewModel.deleteKeyword(kw) }
                                    )
                                }
                            }
                        }
                    }

                    // Add new Keyword input Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = textQueryKeyword,
                            onValueChange = { textQueryKeyword = it },
                            placeholder = { Text("e.g. bonus, lucky, crypto", fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("keyword_input_field"),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (textQueryKeyword.trim().isNotEmpty()) {
                                    viewModel.addKeyword(textQueryKeyword.trim())
                                    textQueryKeyword = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_keyword_button")
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }

        // SECTION C: AUTOMATIC TIME SCHEDULE RECOGNITION
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Schedule Auto-Trigger Quiet-Hour",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Set specific daily time periods where Focus Mode is initiated automatically.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Linked Focus Mode selection
                    Text("1. Linked Focus Preset Mode:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(focusModes) { mode ->
                            val isSelected = selectedLinkedFocusModeId == mode.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedLinkedFocusModeId = mode.id },
                                label = { Text(mode.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Hour and minute fields
                    Text("2. Alert Intercept Window:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = scheduleStartHour,
                            onValueChange = { if (it.length <= 2) scheduleStartHour = it },
                            placeholder = { Text("08", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Text(":")
                        OutlinedTextField(
                            value = scheduleStartMinute,
                            onValueChange = { if (it.length <= 2) scheduleStartMinute = it },
                            placeholder = { Text("00", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Text("to")
                        OutlinedTextField(
                            value = scheduleEndHour,
                            onValueChange = { if (it.length <= 2) scheduleEndHour = it },
                            placeholder = { Text("17", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Text(":")
                        OutlinedTextField(
                            value = scheduleEndMinute,
                            onValueChange = { if (it.length <= 2) scheduleEndMinute = it },
                            placeholder = { Text("30", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = activeSchedulePresetName,
                        onValueChange = { activeSchedulePresetName = it },
                        placeholder = { Text("Schedule Label (e.g. Study Session)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val sh = scheduleStartHour.toIntOrNull() ?: 8
                            val sm = scheduleStartMinute.toIntOrNull() ?: 0
                            val eh = scheduleEndHour.toIntOrNull() ?: 17
                            val em = scheduleEndMinute.toIntOrNull() ?: 30
                            val targetFocusId = selectedLinkedFocusModeId ?: focusModes.firstOrNull()?.id ?: 1L

                            viewModel.addSchedule(
                                name = activeSchedulePresetName.takeIf { it.isNotEmpty() } ?: "Auto Mode",
                                startH = sh,
                                startM = sm,
                                endH = eh,
                                endM = em,
                                linkedFocusId = targetFocusId
                            )

                            // Clear
                            activeSchedulePresetName = ""
                            scheduleStartHour = ""
                            scheduleStartMinute = ""
                            scheduleEndHour = ""
                            scheduleEndMinute = ""
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Activate Automatic Schedule")
                    }
                }
            }
        }

        // SECTION D: THE INTERACTIVE SYSTEM SIMULATOR (FABULOUS FEATURE)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.BugReport, contentDescription = "Sim Bug logo", tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "Device Notification Intercept Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "Manually dispatch notification payloads. The local MyNotification intercept algorithms rule engine will filter, category tags or mute it securely.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Presets horizontal selector list
                    Text("Choose presets payload:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val simPresets = listOf(
                            Triple("Instagram DM", "Instagram", "Hey! Ready for coffee? ☕"),
                            Triple("Slack Urgent", "Slack", "DATABASE DOWN ALERT! Emergency details attached"),
                            Triple("Banking OTP", "WhatsApp", "Your 6-digit verification code is 492048. Expires in 10m."),
                            Triple("Promo Coupon", "Gmail", "HURRY up! Limited 50% discount sale on shoes!")
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(simPresets) { (label, app, bodyText) ->
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(30.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(30.dp))
                                        .clickable {
                                            simAppLabel = app
                                            simTitle = if (app == "Instagram") "sarah_k" else if (app == "Slack") "devops-critical" else "OTP Alert"
                                            simBody = bodyText
                                            simChannel = if (app == "Instagram") "direct" else "general"
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))

                    // Input fields for Custom values
                    OutlinedTextField(
                        value = simAppLabel,
                        onValueChange = { simAppLabel = it },
                        label = { Text("Simulating Application Label") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = simTitle,
                        onValueChange = { simTitle = it },
                        label = { Text("Alert Title Heading") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = simBody,
                        onValueChange = { simBody = it },
                        label = { Text("Notification Text Body") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dispatch Button
                    Button(
                        onClick = {
                            val targetPackage = when (simAppLabel.lowercase()) {
                                "instagram" -> "com.instagram.android"
                                "whatsapp" -> "com.whatsapp"
                                "slack" -> "com.slack"
                                "gmail" -> "com.google.android.gm"
                                else -> "com.aistudio.mock.app"
                            }
                            viewModel.simulateNotification(
                                pkg = targetPackage,
                                label = simAppLabel,
                                title = simTitle,
                                body = simBody,
                                channel = simChannel
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_simulation_button")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send trigger icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dispatch Mock Notification Alert")
                    }
                }
            }
        }
    }
}

// ==========================================
// POPUPS & EXTRA COMPONENT DIALOGS
// ==========================================
@Composable
fun SimulationAlertDialog(
    alert: SimulationAlert,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (alert.isAllowed) Color(0xFF4CAF50) else Color(0xFFE91E63)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (alert.isAllowed) Icons.Filled.Check else Icons.Filled.Lock,
                        contentDescription = "Status",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "System Intercept Outcome",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "[Notification Payload]",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "App: ${alert.appLabel}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Title: ${alert.title}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Body: ${alert.body}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Decision Rule Status:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (alert.isAllowed) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (alert.isAllowed) "ALLOWED" else "INTERCEPTED & MUTED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (alert.isAllowed) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Category: ${alert.category}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Text(
                        text = "Reason: ${alert.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    if (!alert.isAllowed) {
                        Text(
                            text = "💡 Saved safely to your local Vault Inbox.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_simulation_dialog")
            ) {
                Text("Acknowledge")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateFocusModeDialog(
    allAppsList: List<String>,
    onSubmit: (String, String, String, List<String>, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("school") }
    var selectedColor by remember { mutableStateOf("#3F51B5") }

    val selectedAllowed = remember { mutableStateListOf<String>() }
    val selectedBlocked = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Customize Focus Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Mode name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Focus Mode Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Mode Icon options lists
                Text("Select Icon:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val iconOptions = listOf("school", "bedtime", "work", "church", "description", "sports_esports")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(iconOptions) { icName ->
                        val isSelected = selectedIcon == icName
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedIcon = icName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                getVectorIcon(icName),
                                contentDescription = icName,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Mode Color Options lists
                Text("Select Theme Color Accent:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val colorOptions = listOf("#3F51B5", "#9C27B0", "#4CAF50", "#009688", "#F44336", "#FF9800")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colorOptions) { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(hex)))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                // Apps Allowed Flow check lists
                Text("Allow these apps during Focus:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allAppsList.forEach { appName ->
                        val isAllowed = selectedAllowed.contains(appName)
                        FilterChip(
                            selected = isAllowed,
                            onClick = {
                                if (isAllowed) selectedAllowed.remove(appName)
                                else selectedAllowed.add(appName)
                            },
                            label = { Text(appName, fontSize = 10.sp) }
                        )
                    }
                }

                // Actions buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty()) {
                                onSubmit(
                                    name.trim(), selectedIcon, selectedColor,
                                    selectedAllowed.toList(), selectedBlocked.toList()
                                )
                            }
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}
