package co.jp.kpbr.gomiman

import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.model.GarbageType
import co.jp.kpbr.gomiman.utils.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun testJapaneseEraConversion() {
        assertEquals("令和 8年", DateUtils.convertJPYear(2026))
        assertEquals("令和 6年", DateUtils.convertJPYear(2024))
        assertEquals("平成 30年", DateUtils.convertJPYear(2018))
        assertEquals("昭和 60年", DateUtils.convertJPYear(1985))
    }

    @Test
    fun testWeekdayOfMonth() {
        // September 14, 2026 is a Monday (1)
        val testDate = LocalDate.of(2026, 9, 14)
        assertEquals(1, testDate.dayOfWeek.value) // Monday

        // September 2026: 1st is Tuesday(2), so Mondays are:
        // 1st Monday: Sep 7
        // 2nd Monday: Sep 14
        val occurrence = DateUtils.getWeekdayOfMonth(testDate, 1)
        assertEquals(2, occurrence)

        // Today is not Tuesday
        assertEquals(0, DateUtils.getWeekdayOfMonth(testDate, 2))
    }

    @Test
    fun testFormatScheduleSummary() {
        val everyWeekModel = GarbageCollectionModel(
            weekStatus = GarbageCollectionModel.WEEK_STATUS_EVERY_WEEK,
            days = mutableListOf(1, 3), // Mon, Wed
            garbageTypes = mutableListOf(1) // 可燃
        )
        val formattedEveryWeek = DateUtils.formatScheduleSummary(everyWeekModel)
        assertEquals("毎週 月・水曜日", formattedEveryWeek)

        val biweeklyModel = GarbageCollectionModel(
            weekStatus = GarbageCollectionModel.WEEK_STATUS_BIWEEKLY,
            weeks = mutableListOf(1, 3),
            days = mutableListOf(2), // Tue
            garbageTypes = mutableListOf(2) // 不燃
        )
        val formattedBiweekly = DateUtils.formatScheduleSummary(biweeklyModel)
        assertEquals("第1週・第3週 火曜日", formattedBiweekly)
    }

    @Test
    fun testGetGarbageTypesForDate() {
        val model = GarbageCollectionModel(
            weekStatus = GarbageCollectionModel.WEEK_STATUS_EVERY_WEEK,
            days = mutableListOf(1), // Monday
            garbageTypes = mutableListOf(1, 5) // 可燃, プラ
        )
        val monday = LocalDate.of(2026, 9, 14)
        val types = DateUtils.getGarbageTypesForDate(monday, listOf(model))
        assertEquals(2, types.size)
        assertTrue(types.contains(GarbageType.BURNABLE))
        assertTrue(types.contains(GarbageType.PLASTIC))
    }
}
