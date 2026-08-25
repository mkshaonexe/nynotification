package com.quietinbox.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import com.quietinbox.feature.home.HomeScreen
import com.quietinbox.feature.inbox.InboxScreen
import com.quietinbox.feature.onboarding.OnboardingScreen
import com.quietinbox.feature.permissions.PermissionsHealthScreen
import com.quietinbox.feature.schedules.ScheduleEditScreen
import com.quietinbox.feature.schedules.SchedulesScreen
import com.quietinbox.feature.settings.AboutScreen
import com.quietinbox.feature.settings.AppLockScreen
import com.quietinbox.feature.settings.PrivacyScreen
import com.quietinbox.feature.settings.SettingsScreen
import com.quietinbox.feature.settings.StorageRetentionScreen
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
                OnboardingScreen(
                    onNavigateToHome = {
                        navController.navigate(Home) {
                            popUpTo(Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            // 2. Home
            composable<Home> {
                HomeScreen(
                    onNavigateToPermissionsHealth = { navController.navigate(PermissionsHealth) },
                    onNavigateToInboxDay = { day -> navController.navigate(InboxFiltered(day = day)) }
                )
            }

            // 3. Inbox
            composable<Inbox> {
                InboxScreen()
            }

            // 4. InboxFiltered
            composable<InboxFiltered> {
                InboxScreen()
            }

            // 5. NotificationDetail
            composable<NotificationDetail> {
                InboxScreen()
            }

            // 6. Settings
            composable<Settings> {
                SettingsScreen(
                    onNavigateToPermissionsHealth = { navController.navigate(PermissionsHealth) },
                    onNavigateToAllowRules = { navController.navigate(AllowRules) },
                    onNavigateToSchedules = { navController.navigate(Schedules) },
                    onNavigateToMutedApps = { navController.navigate(MutedApps) },
                    onNavigateToStorage = { navController.navigate(Storage) },
                    onNavigateToAppLock = { navController.navigate(AppLock) },
                    onNavigateToPrivacy = { navController.navigate(Privacy) },
                    onNavigateToAbout = { navController.navigate(About) }
                )
            }

            // 7. PermissionsHealth
            composable<PermissionsHealth> {
                PermissionsHealthScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 8. AllowRules
            composable<AllowRules> {
                StubScreen(
                    title = "Always Allow Rules",
                    subtitle = "Rules for apps, senders, and words.",
                    onAction = { navController.popBackStack() },
                    actionLabel = "Back"
                )
            }

            // 9. Schedules
            composable<Schedules> {
                SchedulesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(ScheduleEdit(id)) }
                )
            }

            // 10. ScheduleEdit
            composable<ScheduleEdit> {
                ScheduleEditScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 11. MutedApps
            composable<MutedApps> {
                StubScreen(
                    title = "Muted Apps",
                    subtitle = "Apps that are always silenced.",
                    onAction = { navController.popBackStack() },
                    actionLabel = "Back"
                )
            }

            // 12. Storage
            composable<Storage> {
                StorageRetentionScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 13. AppLock
            composable<AppLock> {
                AppLockScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 14. Privacy
            composable<Privacy> {
                PrivacyScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // 15. About
            composable<About> {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRerunOnboarding = {
                        navController.navigate(Onboarding) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
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
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxSize()
            .padding(QuietTheme.tokens.screenHorizontalPadding)
    ) {
        com.quietinbox.ui.components.SectionHeader(title = title)
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            com.quietinbox.ui.components.EmptyState(
                title = title,
                subtitle = subtitle,
                icon = androidx.compose.material.icons.Icons.Default.Settings,
                actionLabel = actionLabel,
                onActionClick = onAction
            )
        }
    }
}
