package com.quietinbox.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.quietinbox.ui.components.EmptyState
import com.quietinbox.ui.components.SectionHeader
import com.quietinbox.ui.theme.QuietTheme

sealed class BottomNavItem(val route: Any, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object HomeItem : BottomNavItem(Home, "Home", Icons.Default.Home)
    data object InboxItem : BottomNavItem(Inbox, "Inbox", Icons.Default.Inbox)
    data object SettingsItem : BottomNavItem(Settings, "Settings", Icons.Default.Settings)
}

private val bottomNavItems = listOf(
    BottomNavItem.HomeItem,
    BottomNavItem.InboxItem,
    BottomNavItem.SettingsItem
)

@Composable
fun QuietNavHost(
    onboardingCompleted: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val isTopLevel = bottomNavItems.any { item ->
        currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isTopLevel) {
                NavigationBar(
                    containerColor = QuietTheme.colors.surface,
                    contentColor = QuietTheme.colors.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.hasRoute(item.route::class) } == true
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, style = QuietTheme.typography.label) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = QuietTheme.colors.primary,
                                selectedTextColor = QuietTheme.colors.primary,
                                unselectedIconColor = QuietTheme.colors.onSurfaceVariant,
                                unselectedTextColor = QuietTheme.colors.onSurfaceVariant,
                                indicatorColor = QuietTheme.colors.surfaceVariant
                            ),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingCompleted) Home else Onboarding,
            modifier = Modifier
                .fillMaxSize()
                .background(QuietTheme.colors.background)
                .padding(innerPadding)
        ) {
            // 1. Onboarding
            composable<Onboarding> {
                StubScreen(
                    title = "Onboarding",
                    subtitle = "Welcome to Quiet Inbox. Phase 8 will implement the 4-step onboarding flow.",
                    onAction = {
                        navController.navigate(Home) {
                            popUpTo(Onboarding) { inclusive = true }
                        }
                    },
                    actionLabel = "Enter App (Skip to Home)"
                )
            }

            // 2. Home
            composable<Home> {
                StubScreen(
                    title = "Home",
                    subtitle = "Quiet Switch, stats tiles, and activity heatmap. Phase 5 will implement this."
                )
            }

            // 3. Inbox
            composable<Inbox> {
                StubScreen(
                    title = "Inbox",
                    subtitle = "Search, day grouped notifications, filters. Phase 4 will implement this."
                )
            }

            // 4. InboxFiltered
            composable<InboxFiltered> { backStackEntry ->
                val args = backStackEntry.toRoute<InboxFiltered>()
                StubScreen(
                    title = "Inbox Filtered",
                    subtitle = "Filtered by package=${args.packageName}, day=${args.day}. Phase 4 will implement this."
                )
            }

            // 5. NotificationDetail
            composable<NotificationDetail> { backStackEntry ->
                val args = backStackEntry.toRoute<NotificationDetail>()
                StubScreen(
                    title = "Notification Detail",
                    subtitle = "Detail view for notification id=${args.id}. Phase 4 will implement this."
                )
            }

            // 6. Settings
            composable<Settings> {
                StubScreen(
                    title = "Settings",
                    subtitle = "Main settings hub. Phase 9 will implement this."
                )
            }

            // 7. PermissionsHealth
            composable<PermissionsHealth> {
                StubScreen(
                    title = "Permissions & Health",
                    subtitle = "Diagnostics and system permissions manager. Phase 8 will implement this."
                )
            }

            // 8. AllowRules
            composable<AllowRules> {
                StubScreen(
                    title = "Always Allow Rules",
                    subtitle = "Rules for apps, senders, and words. Phase 6 will implement this."
                )
            }

            // 9. Schedules
            composable<Schedules> {
                StubScreen(
                    title = "Schedules",
                    subtitle = "Scheduled quiet and open time windows. Phase 7 will implement this."
                )
            }

            // 10. ScheduleEdit
            composable<ScheduleEdit> { backStackEntry ->
                val args = backStackEntry.toRoute<ScheduleEdit>()
                StubScreen(
                    title = "Edit Schedule",
                    subtitle = "Editing schedule id=${args.id}. Phase 7 will implement this."
                )
            }

            // 11. MutedApps
            composable<MutedApps> {
                StubScreen(
                    title = "Muted Apps",
                    subtitle = "Apps that are always silenced. Phase 6 will implement this."
                )
            }

            // 12. Storage
            composable<Storage> {
                StubScreen(
                    title = "Storage & Retention",
                    subtitle = "Retention policies and database cleanup. Phase 9 will implement this."
                )
            }

            // 13. AppLock
            composable<AppLock> {
                StubScreen(
                    title = "App Lock",
                    subtitle = "Biometric and PIN security lock. Phase 9 will implement this."
                )
            }

            // 14. Privacy
            composable<Privacy> {
                StubScreen(
                    title = "Privacy",
                    subtitle = "Offline-first guarantee and local data policies. Phase 9 will implement this."
                )
            }

            // 15. About
            composable<About> {
                StubScreen(
                    title = "About",
                    subtitle = "Quiet Inbox version info and diagnostics. Phase 9 will implement this."
                )
            }
        }
    }
}

@Composable
fun StubScreen(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(QuietTheme.tokens.screenHorizontalPadding)
    ) {
        SectionHeader(title = title)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            EmptyState(
                title = "Under Construction",
                subtitle = subtitle,
                icon = Icons.Default.Construction,
                actionLabel = actionLabel,
                onActionClick = onAction
            )
        }
    }
}
