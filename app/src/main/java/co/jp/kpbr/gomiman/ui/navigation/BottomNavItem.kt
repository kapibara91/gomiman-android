package co.jp.kpbr.gomiman.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Timeline : BottomNavItem(
        route = NavRoute.Timeline.route,
        title = "カレンダー",
        icon = Icons.Outlined.DateRange
    )

    data object GarbageList : BottomNavItem(
        route = NavRoute.GarbageList.route,
        title = "ゴミの日",
        icon = Icons.Outlined.Delete
    )

    data object Settings : BottomNavItem(
        route = NavRoute.Settings.route,
        title = "設定",
        icon = Icons.Outlined.Settings
    )

    companion object {
        val items = listOf(Timeline, GarbageList, Settings)
    }
}
