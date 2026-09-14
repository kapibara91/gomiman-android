package co.jp.kpbr.gomiman.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class CalendarRepository(private val context: Context) {

    companion object {
        const val CALENDAR_EVENT_PREFIX = "【ゴミマン】"
        const val CALENDAR_EVENT_DESCRIPTION = "ゴミマンによって登録されたごみ収集予定"
    }

    fun hasCalendarPermission(): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    private fun getPrimaryCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.VISIBLE
        )
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )
        cursor?.use { c ->
            val idCol = c.getColumnIndex(CalendarContract.Calendars._ID)
            val primaryCol = c.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

            var firstId: Long? = null
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                if (firstId == null) firstId = id
                if (primaryCol != -1 && c.getInt(primaryCol) == 1) {
                    return id
                }
            }
            return firstId
        }
        return null
    }

    /**
     * Registers garbage events into Android Calendar.
     * @param models Garbage schedule definitions.
     * @param periodMonths Number of months to schedule (e.g. 0: 7 days, 1: 30 days, 2: 60 days).
     * @param collectionDateFlag 0: Day before, 1: Day of collection.
     * @param selectedHour Hour of the day (e.g. 19..23 or 5..9).
     * @param hasNotification Whether to attach an alarm reminder.
     * @return Number of events added.
     */
    suspend fun registerEvents(
        models: List<GarbageCollectionModel>,
        periodMonths: Int,
        collectionDateFlag: Int,
        selectedHour: Int,
        hasNotification: Boolean
    ): Int = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext 0

        val calendarId = getPrimaryCalendarId() ?: return@withContext 0
        val daysCount = when (periodMonths) {
            0 -> 7
            1 -> 30
            2 -> 60
            else -> 30
        }

        var addedCount = 0
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        for (i in 0 until daysCount) {
            val targetDate = today.plusDays(i.toLong())
            val types = DateUtils.getGarbageTypesForDate(targetDate, models)
            if (types.isNotEmpty()) {
                val eventDate = if (collectionDateFlag == 0) targetDate.minusDays(1) else targetDate
                val eventDateTime = eventDate.atTime(selectedHour, 0)
                val startMillis = eventDateTime.atZone(zoneId).toInstant().toEpochMilli()
                val endMillis = startMillis + (30 * 60 * 1000) // 30 minutes duration

                val title = "$CALENDAR_EVENT_PREFIX " + types.joinToString(", ") { it.typeName }

                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.DESCRIPTION, CALENDAR_EVENT_DESCRIPTION)
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }

                val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                if (eventUri != null) {
                    addedCount++
                    if (hasNotification) {
                        val eventId = ContentUris.parseId(eventUri)
                        val reminderValues = ContentValues().apply {
                            put(CalendarContract.Reminders.MINUTES, 0) // At the time of event
                            put(CalendarContract.Reminders.EVENT_ID, eventId)
                            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                        }
                        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                    }
                }
            }
        }
        addedCount
    }

    suspend fun resetEvents(): Int = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext 0

        val selection = "${CalendarContract.Events.TITLE} LIKE ?"
        val selectionArgs = arrayOf("$CALENDAR_EVENT_PREFIX%")

        return@withContext context.contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,
            selection,
            selectionArgs
        )
    }
}
