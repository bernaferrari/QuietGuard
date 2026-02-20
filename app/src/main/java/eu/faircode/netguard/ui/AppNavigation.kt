package eu.faircode.netguard.ui

import androidx.annotation.StringRes
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.navigation3.ui.defaultPredictivePopTransitionSpec
import androidx.navigation3.ui.defaultTransitionSpec
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import eu.faircode.netguard.R
import eu.faircode.netguard.ui.main.AppsScreen
import eu.faircode.netguard.ui.main.HomeScreen
import eu.faircode.netguard.ui.main.MainViewModel
import eu.faircode.netguard.ui.screens.AppRuleDetailScreen
import eu.faircode.netguard.ui.screens.DnsScreen
import eu.faircode.netguard.ui.screens.ForwardingScreen
import eu.faircode.netguard.ui.screens.LogsScreen
import eu.faircode.netguard.ui.screens.ProScreen
import eu.faircode.netguard.ui.screens.SettingsScreen

private enum class NavDestination(
    val key: AppNavKey,
    @param:StringRes val label: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    HomeTab(Home, R.string.menu_home, Icons.Default.Security),
    AppsTab(Apps, R.string.menu_firewall, Icons.Default.Tune),
    LogsTab(Logs, R.string.menu_log, Icons.AutoMirrored.Filled.List),
    SettingsTab(Settings, R.string.menu_settings, Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    onToggleEnabled: (Boolean) -> Unit,
    startRoute: String,
    pendingRoute: String? = null,
    onRouteNavigated: () -> Unit = {},
) {
    val startKey = remember(startRoute) { NavRoutes.fromRoute(startRoute) }
    val backStack = rememberNavBackStack(startKey)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
    fun Scene<NavKey>.hasAppDetailEntry(): Boolean {
        return entries.any { entry ->
            entry.contentKey.toString().startsWith("AppRuleDetail(")
        }
    }
    fun popBackStack() {
        if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun setStack(vararg keys: AppNavKey) {
        backStack.clear()
        backStack.addAll(keys.toList())
    }

    fun navigateTo(destination: AppNavKey) {
        when (destination) {
            Logs -> setStack(Apps, Logs)
            Dns -> setStack(Settings, Dns)
            Forwarding -> setStack(Settings, Forwarding)
            Pro -> setStack(Settings, Pro)
            else -> setStack(destination)
        }
    }

    fun selectedTabFor(current: NavKey?): AppNavKey? {
        val appKey = current as? AppNavKey ?: return null
        return when (appKey) {
            is AppRuleDetail -> Apps
            Dns, Forwarding, Pro -> Settings
            else -> appKey
        }
    }

    LaunchedEffect(pendingRoute) {
        if (!pendingRoute.isNullOrBlank()) {
            navigateTo(NavRoutes.fromRoute(pendingRoute))
            onRouteNavigated()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                val currentKey = backStack.lastOrNull()
                val selectedTab = selectedTabFor(currentKey)
                NavDestination.entries.forEach { destination ->
                    val selected = selectedTab == destination.key
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateTo(destination.key) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.label),
                            )
                        },
                        label = {
                            Text(stringResource(destination.label))
                        },
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                    )
                }
            }
        },
    ) { padding ->
        val overlayDetail = backStack.lastOrNull() as? AppRuleDetail
        val baseBackStack =
            if (overlayDetail != null && backStack.size > 1) {
                backStack.dropLast(1)
            } else {
                backStack.toList()
            }
        val rulesUiState by viewModel.rulesUiState.collectAsStateWithLifecycle()

        if (overlayDetail != null) {
            LaunchedEffect(overlayDetail.uid) {
                viewModel.ensureRulesLoaded()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            NavDisplay(
                backStack = baseBackStack,
                modifier = Modifier.fillMaxSize(),
                sceneStrategy = listDetailStrategy,
                onBack = { popBackStack() },
                transitionSpec = {
                    val isAppDetailTransition =
                        initialState.hasAppDetailEntry() || targetState.hasAppDetailEntry()
                    if (isAppDetailTransition) {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    } else {
                        defaultTransitionSpec<NavKey>()(this)
                    }
                },
                popTransitionSpec = {
                    val isAppDetailTransition =
                        initialState.hasAppDetailEntry() || targetState.hasAppDetailEntry()
                    if (isAppDetailTransition) {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    } else {
                        defaultPopTransitionSpec<NavKey>()(this)
                    }
                },
                predictivePopTransitionSpec = { swipeEdge ->
                    val isAppDetailTransition =
                        initialState.hasAppDetailEntry() || targetState.hasAppDetailEntry()
                    if (isAppDetailTransition) {
                        ContentTransform(EnterTransition.None, ExitTransition.None)
                    } else {
                        defaultPredictivePopTransitionSpec<NavKey>()(this, swipeEdge)
                    }
                },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider =
                    entryProvider {
                        entry<Home> {
                            HomeScreen(
                                viewModel = viewModel,
                                onToggleEnabled = onToggleEnabled,
                            )
                        }
                        entry<Apps>(
                            metadata =
                                ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        Text(
                                            text = stringResource(R.string.home_logs_hint),
                                            modifier = Modifier.padding(24.dp),
                                        )
                                    },
                                ),
                        ) {
                            AppsScreen(
                                viewModel = viewModel,
                                onNavigateToDetail = { rule ->
                                    backStack.add(AppRuleDetail(rule.uid))
                                },
                            )
                        }
                        entry<AppRuleDetail>(
                            metadata = ListDetailSceneStrategy.detailPane(),
                        ) { key ->
                            // Kept for compatibility if route stack explicitly resolves to detail scene.
                            val targetRule = rulesUiState.rules.firstOrNull { it.uid == key.uid }
                            if (targetRule != null) {
                                AppRuleDetailScreen(
                                    rule = targetRule,
                                    allRules = rulesUiState.rules,
                                    onRuleChanged = { viewModel.notifyRulesChanged() },
                                    onBack = { popBackStack() },
                                )
                            }
                        }
                        entry<Logs>(
                            metadata = ListDetailSceneStrategy.detailPane(),
                        ) {
                            LogsScreen()
                        }
                        entry<Settings> {
                            SettingsScreen(
                                onOpenDns = { navigateTo(Dns) },
                                onOpenForwarding = { navigateTo(Forwarding) },
                                onOpenPro = { navigateTo(Pro) },
                            )
                        }
                        entry<Dns> {
                            DnsScreen()
                        }
                        entry<Forwarding> {
                            ForwardingScreen()
                        }
                        entry<Pro> {
                            ProScreen()
                        }
                    },
            )

            if (overlayDetail != null) {
                val targetRule = rulesUiState.rules.firstOrNull { it.uid == overlayDetail.uid }
                if (targetRule != null) {
                    AppRuleDetailScreen(
                        rule = targetRule,
                        allRules = rulesUiState.rules,
                        onRuleChanged = { viewModel.notifyRulesChanged() },
                        onBack = { popBackStack() },
                    )
                }
            }
        }
    }
}
