package co.jp.kpbr.gomiman.data.model

data class GarbageCollectionModel(
    var id: Long? = null,
    /** 1: 毎週 (Every week), 2: 隔週 (Bi-weekly) */
    var weekStatus: Int = 1,
    /** Weeks: 1 to 5 */
    var weeks: MutableList<Int> = mutableListOf(),
    /** Garbage types: 1 to 7 */
    var garbageTypes: MutableList<Int> = mutableListOf(),
    /** Days of week: 1 (Mon) to 7 (Sun) */
    var days: MutableList<Int> = mutableListOf()
) {
    companion object {
        const val WEEK_STATUS_EVERY_WEEK = 1
        const val WEEK_STATUS_BIWEEKLY = 2
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "weekStatus" to weekStatus,
            "weeks" to weeks.toList(),
            "garbageTypes" to garbageTypes.toList(),
            "days" to days.toList()
        )
    }
}
