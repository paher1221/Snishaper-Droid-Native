package com.snishaper.android.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.snishaper.android.feature.connections.ConnectionsScreen
import com.snishaper.android.feature.dashboard.DashboardScreen
import com.snishaper.android.feature.dns.DnsScreen
import com.snishaper.android.feature.evolution.EvolutionScreen
import com.snishaper.android.feature.logs.LogsScreen
import com.snishaper.android.feature.onboarding.OnboardingScreen
import com.snishaper.android.feature.proxies.ProxiesScreen
import com.snishaper.android.feature.profiles.ProfilesScreen
import com.snishaper.android.feature.rules.RulesScreen
import com.snishaper.android.feature.routing.RoutingScreen
import com.snishaper.android.feature.settings.SettingsScreen

/**
 * 导航图：11 个目的地，与桌面版 Sidebar 顺序一致。
 * M0 为占位壳；各 feature 的真实 UI 随里程碑落地。
 */
private data class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val destinations = listOf(
    Destination("dashboard", "仪表盘", Icons.Outlined.Home),
    Destination("connections", "连接", Icons.Outlined.ListAlt),
    Destination("rules", "规则", Icons.Outlined.ListAlt),
    Destination("routing", "自动分流", Icons.Outlined.ListAlt),
    Destination("dns", "DNS", Icons.Outlined.Dns),
    Destination("proxies", "代理", Icons.Outlined.Dns),
    Destination("evolution", "演进模式", Icons.Outlined.ListAlt),
    Destination("logs", "日志", Icons.Outlined.ListAlt),
    Destination("profiles", "档案", Icons.Outlined.ListAlt),
    Destination("settings", "设置", Icons.Outlined.Settings),
    Destination("onboarding", "关于", Icons.Filled.Info),
)

@Composable
fun SnishaperApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "dashboard"

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                destinations.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.label) },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) {
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                NavHost(navController = navController, startDestination = "dashboard") {
                    composable("dashboard") { DashboardScreen() }
                    composable("connections") { ConnectionsScreen() }
                    composable("rules") { RulesScreen() }
                    composable("routing") { RoutingScreen() }
                    composable("dns") { DnsScreen() }
                    composable("proxies") { ProxiesScreen() }
                    composable("evolution") { EvolutionScreen() }
                    composable("logs") { LogsScreen() }
                    composable("profiles") { ProfilesScreen() }
                    composable("settings") { SettingsScreen() }
                    composable("onboarding") { OnboardingScreen() }
                }
            }
        }
    }
}
