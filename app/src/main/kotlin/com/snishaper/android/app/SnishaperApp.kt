package com.snishaper.android.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.snishaper.android.feature.connections.ui.ConnectionsScreen
import com.snishaper.android.feature.dashboard.ui.DashboardScreen
import com.snishaper.android.feature.dns.DnsScreen
import com.snishaper.android.feature.evolution.EvolutionScreen
import com.snishaper.android.feature.logs.ui.LogsScreen
import com.snishaper.android.feature.onboarding.OnboardingScreen
import com.snishaper.android.feature.proxies.ProxiesScreen
import com.snishaper.android.feature.profiles.ProfilesScreen
import com.snishaper.android.feature.rules.ui.RulesScreen
import com.snishaper.android.feature.routing.ui.RoutingScreen
import com.snishaper.android.feature.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * 导航图：11 个目的地，与桌面版 Sidebar 顺序一致。
 * 侧边栏只占半屏（fillMaxWidth(0.5f)），点击条目后收起抽屉再进入页面；
 * 顶栏汉堡按钮随时可唤出。
 */
private data class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/** 页面切换动画时长（毫秒），与系统原生过渡节奏一致。 */
private const val NAV_ANIM_MS = 300

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnishaperApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: "dashboard"
    val currentLabel = destinations.firstOrNull { it.route == currentRoute }?.label ?: "SniShaper"

    val drawerState = rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.5f)) {
                destinations.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.label) },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        selected = currentRoute == dest.route,
                        onClick = {
                            scope.launch { drawerState.close() }
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
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(currentLabel, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "菜单")
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                // 原生风格的水平滑动过渡（Material 共享轴 X）：
                // 前进 = 新页从右滑入、旧页向左淡出；返回 = 反向。
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    enterTransition = {
                        slideInHorizontally(tween(NAV_ANIM_MS)) { it } +
                            fadeIn(tween(NAV_ANIM_MS))
                    },
                    exitTransition = {
                        slideOutHorizontally(tween(NAV_ANIM_MS)) { -it / 3 } +
                            fadeOut(tween(NAV_ANIM_MS))
                    },
                    popEnterTransition = {
                        slideInHorizontally(tween(NAV_ANIM_MS)) { -it / 3 } +
                            fadeIn(tween(NAV_ANIM_MS))
                    },
                    popExitTransition = {
                        slideOutHorizontally(tween(NAV_ANIM_MS)) { it } +
                            fadeOut(tween(NAV_ANIM_MS))
                    },
                ) {
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
