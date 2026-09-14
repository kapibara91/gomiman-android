package co.jp.kpbr.gomiman.data.model

data class PushSettingModel(
    val collectionDayBefore: Boolean = true,
    /** 0: 19:00, 1: 20:00, 2: 21:00, 3: 22:00, 4: 23:00 */
    val selectedTimeDayBefore: Int = 1,
    val collectionDayAfter: Boolean = false,
    /** 0: 05:00, 1: 06:00, 2: 07:00, 3: 08:00, 4: 09:00 */
    val selectedTimeDayAfter: Int = 0
) {
    fun getDayBeforeHour(): Int = when (selectedTimeDayBefore) {
        0 -> 19
        1 -> 20
        2 -> 21
        3 -> 22
        4 -> 23
        else -> 20
    }

    fun getDayAfterHour(): Int = when (selectedTimeDayAfter) {
        0 -> 5
        1 -> 6
        2 -> 7
        3 -> 8
        4 -> 9
        else -> 5
    }
}
