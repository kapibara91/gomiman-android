package co.jp.kpbr.gomiman.ui.navigation

sealed class NavRoute(val route: String) {
    data object Timeline : NavRoute("timeline")
    data object GarbageList : NavRoute("garbage_list")
    data object Settings : NavRoute("settings")

    data object GarbageAdd : NavRoute("garbage_add")
    data object PushSettings : NavRoute("push_settings")
    data object CalendarAppend : NavRoute("calendar_append")
    data object Feedback : NavRoute("feedback")
}
