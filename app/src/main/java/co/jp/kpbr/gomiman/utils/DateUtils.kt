package co.jp.kpbr.gomiman.utils

import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.model.GarbageType
import java.time.LocalDate

object DateUtils {

    val weekdayInHanji = listOf("", "月", "火", "水", "木", "金", "土", "日")

    fun convertJPYear(year: Int): String {
        return when {
            year < 1868 -> "明治より前です"
            year < 1912 -> "明治 ${year - 33 - 1900}年"
            year < 1926 -> "大正 ${year - 11 - 1900}年"
            year < 1989 -> "昭和 ${year - 25 - 1900}年"
            year < 2019 -> "平成 ${year + 12 - 2000}年"
            else -> "令和 ${year - 18 - 2000}年"
        }
    }

    /**
     * Checks which occurrence (1st, 2nd, etc.) of a weekday the given date is in its month.
     * @param date The date to inspect.
     * @param targetWeekday 1 (Mon) to 7 (Sun).
     * @return 1..5 if today matches targetWeekday, or 0 if not.
     */
    fun getWeekdayOfMonth(date: LocalDate, targetWeekday: Int): Int {
        if (date.dayOfWeek.value != targetWeekday) {
            return 0
        }
        val firstDayOfMonth = date.withDayOfMonth(1)
        val firstWeekday = if (firstDayOfMonth.dayOfWeek.value <= targetWeekday) {
            targetWeekday - firstDayOfMonth.dayOfWeek.value + 1
        } else {
            targetWeekday - firstDayOfMonth.dayOfWeek.value + 1 + 7
        }
        return (date.dayOfMonth - firstWeekday) / 7 + 1
    }

    /**
     * Finds all garbage types scheduled for a specific date.
     */
    fun getGarbageTypesForDate(date: LocalDate, models: List<GarbageCollectionModel>): List<GarbageType> {
        val result = mutableSetOf<GarbageType>()
        val dayOfWeek = date.dayOfWeek.value // 1 (Mon) to 7 (Sun)

        for (model in models) {
            if (model.days.contains(dayOfWeek)) {
                if (model.weekStatus == GarbageCollectionModel.WEEK_STATUS_EVERY_WEEK) {
                    for (typeId in model.garbageTypes) {
                        GarbageType.fromId(typeId)?.let { result.add(it) }
                    }
                } else if (model.weekStatus == GarbageCollectionModel.WEEK_STATUS_BIWEEKLY) {
                    val occurrence = getWeekdayOfMonth(date, dayOfWeek)
                    if (model.weeks.contains(occurrence)) {
                        for (typeId in model.garbageTypes) {
                            GarbageType.fromId(typeId)?.let { result.add(it) }
                        }
                    }
                }
            }
        }
        return result.sortedBy { it.id }
    }

    /**
     * Formats schedule string like: "毎週 月、水曜日" or "第1、第3 火曜日"
     */
    fun formatScheduleSummary(model: GarbageCollectionModel): String {
        val weeksStr = if (model.weekStatus == GarbageCollectionModel.WEEK_STATUS_EVERY_WEEK) {
            "毎週"
        } else {
            model.weeks.joinToString("、") { "第$it" }
        }

        val daysStr = model.days.mapNotNull {
            if (it in 1..7) weekdayInHanji[it] else null
        }.joinToString("、") + "曜日"

        return "$weeksStr　$daysStr"
    }
}
