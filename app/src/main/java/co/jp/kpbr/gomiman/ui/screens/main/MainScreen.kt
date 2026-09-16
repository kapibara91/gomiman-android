package co.jp.kpbr.gomiman.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.jp.kpbr.gomiman.data.local.PreferencesManager
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.repository.CalendarRepository
import co.jp.kpbr.gomiman.data.repository.GarbageRepository
import co.jp.kpbr.gomiman.data.repository.SyncRepository
import co.jp.kpbr.gomiman.ui.navigation.BottomNavItem
import co.jp.kpbr.gomiman.ui.navigation.NavRoute
import co.jp.kpbr.gomiman.ui.screens.calendarappend.CalendarAppendScreen
import co.jp.kpbr.gomiman.ui.screens.feedback.FeedbackScreen
import co.jp.kpbr.gomiman.ui.screens.garbageadd.GarbageAddScreen
import co.jp.kpbr.gomiman.ui.screens.garbagelist.GarbageListScreen
import co.jp.kpbr.gomiman.ui.screens.pushsettings.PushSettingScreen
import co.jp.kpbr.gomiman.ui.screens.settings.SettingsScreen
import co.jp.kpbr.gomiman.ui.screens.timeline.CalendarTimelineScreen
import co.jp.kpbr.gomiman.ui.theme.DefaultThemeColor
import co.jp.kpbr.gomiman.ui.theme.DividerColor
import co.jp.kpbr.gomiman.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    garbageRepository: GarbageRepository,
    calendarRepository: CalendarRepository,
    preferencesManager: PreferencesManager,
    syncRepository: SyncRepository
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val garbageModels by garbageRepository.garbageModels.collectAsState()

    var showFirstTimeNotificationDialog by remember { mutableStateOf(false) }

    if (showFirstTimeNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showFirstTimeNotificationDialog = false },
            title = null,
            text = {
                Text(
                    text = "ゴミ捨ての通知を受け取りますか。",
                    fontSize = 16.sp,
                    color = DefaultThemeColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFirstTimeNotificationDialog = false
                        navController.navigate(NavRoute.PushSettings.route)
                    }
                ) {
                    Text("OK", color = DefaultThemeColor, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirstTimeNotificationDialog = false }) {
                    Text("キャンセル", color = TextSecondary)
                }
            },
            containerColor = Color.White
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelRoute = currentRoute in BottomNavItem.items.map { it.route }

    Scaffold(
        bottomBar = {
            if (isTopLevelRoute) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    BottomNavItem.items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (selected) DefaultThemeColor else TextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 12.sp,
                                    color = if (selected) DefaultThemeColor else TextSecondary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0.dp),
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            NavHost(
                navController = navController,
                startDestination = NavRoute.Timeline.route
            ) {
                composable(NavRoute.Timeline.route) {
                    CalendarTimelineScreen(
                        garbageModels = garbageModels,
                        onNavigateToAdd = {
                            navController.navigate(NavRoute.GarbageAdd.route)
                        },
                        onNavigateToCalendarAppend = {
                            navController.navigate(NavRoute.CalendarAppend.route)
                        }
                    )
                }

                composable(NavRoute.GarbageList.route) {
                    GarbageListScreen(
                        garbageModels = garbageModels,
                        onNavigateToAdd = {
                            navController.navigate(NavRoute.GarbageAdd.route)
                        },
                        onNavigateToPushSettings = {
                            navController.navigate(NavRoute.PushSettings.route)
                        },
                        onDeleteSchedule = { id ->
                            scope.launch {
                                garbageRepository.deleteGarbageCollection(id)
                                garbageRepository.syncWithServer(syncRepository)
                            }
                        }
                    )
                }

                composable(NavRoute.Settings.route) {
                    SettingsScreen(
                        calendarRepository = calendarRepository,
                        onResetGarbageCollections = {
                            scope.launch {
                                garbageRepository.deleteAllGarbageCollections()
                                garbageRepository.syncWithServer(syncRepository)
                            }
                        },
                        onNavigateToPushSettings = {
                            navController.navigate(NavRoute.PushSettings.route)
                        },
                        onNavigateToCalendarAppend = {
                            navController.navigate(NavRoute.CalendarAppend.route)
                        },
                        onNavigateToFeedback = {
                            navController.navigate(NavRoute.Feedback.route)
                        }
                    )
                }

                composable(NavRoute.GarbageAdd.route) {
                    GarbageAddScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onSaveSuccess = { newModel ->
                            scope.launch {
                                garbageRepository.insertGarbageCollection(newModel)
                                garbageRepository.syncWithServer(syncRepository)
                                if (preferencesManager.isFirstTimeAddedGarbage()) {
                                    preferencesManager.markFirstTimeAddedGarbage()
                                    showFirstTimeNotificationDialog = true
                                }
                                navController.popBackStack()
                            }
                        }
                    )
                }

                composable(NavRoute.CalendarAppend.route) {
                    CalendarAppendScreen(
                        garbageModels = garbageModels,
                        calendarRepository = calendarRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoute.PushSettings.route) {
                    PushSettingScreen(
                        preferencesManager = preferencesManager,
                        syncRepository = syncRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoute.Feedback.route) {
                    FeedbackScreen(
                        syncRepository = syncRepository,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
