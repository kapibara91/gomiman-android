package co.jp.kpbr.gomiman.data.repository

import android.accounts.Account
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset

data class CalendarAccount(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val isPrimary: Boolean,
    val isVisible: Boolean
)

sealed class CalendarRegisterResult {
    data class Success(val count: Int) : CalendarRegisterResult()
    data object NoSchedules : CalendarRegisterResult()
    data object NoCalendarFound : CalendarRegisterResult()
    data object PermissionDenied : CalendarRegisterResult()
}

class CalendarRepository(private val context: Context) {

    companion object {
        const val CALENDAR_EVENT_TAG = "ゴミマン"
        const val CALENDAR_EVENT_SUFFIX = " (ゴミマン)"
        const val CALENDAR_EVENT_OLD_PREFIX = "【ゴミマン】"
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

    /**
     * Retrieves all writable calendars on the device, sorted by preference:
     * 1. Google accounts (com.google) first
     * 2. Visible calendars
     * 3. Primary calendars
     */
    fun getWritableCalendars(): List<CalendarAccount> {
        if (!hasCalendarPermission()) return emptyList()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        val list = mutableListOf<CalendarAccount>()

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )?.use { c ->
                val idCol = c.getColumnIndex(CalendarContract.Calendars._ID)
                val displayCol = c.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val nameCol = c.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val typeCol = c.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val primaryCol = c.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                val visibleCol = c.getColumnIndex(CalendarContract.Calendars.VISIBLE)
                val accessCol = c.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

                while (c.moveToNext()) {
                    val accessLevel = if (accessCol != -1) c.getInt(accessCol) else CalendarContract.Calendars.CAL_ACCESS_NONE
                    if (accessLevel < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                        continue
                    }

                    val id = c.getLong(idCol)
                    val displayName = if (displayCol != -1) c.getString(displayCol) ?: "" else ""
                    val accountName = if (nameCol != -1) c.getString(nameCol) ?: "" else ""
                    val accountType = if (typeCol != -1) c.getString(typeCol) ?: "" else ""
                    val isPrimary = primaryCol != -1 && c.getInt(primaryCol) == 1
                    val isVisible = visibleCol != -1 && c.getInt(visibleCol) == 1

                    list.add(
                        CalendarAccount(
                            id = id,
                            displayName = displayName,
                            accountName = accountName,
                            accountType = accountType,
                            isPrimary = isPrimary,
                            isVisible = isVisible
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CalendarRepository", "getWritableCalendars error: ${e.message}", e)
        }

        return list.sortedWith(
            compareByDescending<CalendarAccount> { it.accountType == "com.google" }
                .thenByDescending { it.isVisible }
                .thenByDescending { it.isPrimary }
        )
    }

    /**
     * Determines the most appropriate default writable calendar.
     * Prioritizes visible primary Google calendars over offline local calendars.
     */
    fun getDefaultCalendar(): CalendarAccount? {
        val calendars = getWritableCalendars()
        if (calendars.isEmpty()) return null

        return calendars.firstOrNull { it.accountType == "com.google" && it.isVisible && it.isPrimary }
            ?: calendars.firstOrNull { it.accountType == "com.google" && it.isVisible }
            ?: calendars.firstOrNull { it.accountType == "com.google" }
            ?: calendars.firstOrNull { it.isVisible && it.isPrimary }
            ?: calendars.firstOrNull { it.isVisible }
            ?: calendars.first()
    }

    /**
     * Registers garbage events into Android Calendar as all-day events on collection days.
     * Cleans up existing Gomiman calendar events first without intermediate sync to prevent race conditions.
     *
     * @param models Garbage schedule definitions.
     * @param periodMonths Number of months to schedule (e.g. 0: 7 days, 1: 30 days, 2: 60 days).
     * @param targetCalendarId Optional target calendar ID; uses getDefaultCalendar if omitted.
     * @return CalendarRegisterResult
     */
    suspend fun registerEvents(
        models: List<GarbageCollectionModel>,
        periodMonths: Int,
        targetCalendarId: Long? = null
    ): CalendarRegisterResult = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext CalendarRegisterResult.PermissionDenied
        }

        if (models.isEmpty()) {
            return@withContext CalendarRegisterResult.NoSchedules
        }

        val calendarId = targetCalendarId ?: getDefaultCalendar()?.id
            ?: return@withContext CalendarRegisterResult.NoCalendarFound

        // Clean up existing Gomiman events locally first WITHOUT triggering sync yet
        // so we do not cause SyncManager throttling or race conditions
        resetEvents(triggerSync = false)

        val daysCount = when (periodMonths) {
            0 -> 7
            1 -> 30
            2 -> 60
            else -> 30
        }

        var addedCount = 0
        val today = LocalDate.now()

        for (i in 0 until daysCount) {
            val targetDate = today.plusDays(i.toLong())
            val types = DateUtils.getGarbageTypesForDate(targetDate, models)
            if (types.isNotEmpty()) {
                // All-day event on collection day (midnight UTC to midnight UTC next day)
                val startMillis = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val endMillis = targetDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

                val title = types.joinToString(", ") { it.typeName } + CALENDAR_EVENT_SUFFIX

                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.ALL_DAY, 1)
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.DESCRIPTION, CALENDAR_EVENT_DESCRIPTION)
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
                    put(CalendarContract.Events.EVENT_END_TIMEZONE, "UTC")
                    put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
                    put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_FREE)
                    put(CalendarContract.Events.HAS_ALARM, 0)
                    put(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
                }

                val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                if (eventUri != null) {
                    addedCount++
                }
            }
        }

        if (addedCount > 0) {
            context.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null)
            context.contentResolver.notifyChange(CalendarContract.Instances.CONTENT_URI, null)
            // Trigger single cloud sync after all deletions & insertions are complete
            requestSyncForCalendars(setOf(calendarId))
        }

        CalendarRegisterResult.Success(count = addedCount)
    }

    /**
     * Deletes all Gomiman-registered events across all calendars.
     * Uses ID-based deletion, notifies observers, and triggers cloud sync for synced accounts.
     *
     * @param triggerSync Whether to trigger expedited cloud sync immediately.
     * @return The number of deleted events.
     */
    suspend fun resetEvents(triggerSync: Boolean = true): Int = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext 0
        }

        try {
            // Match any event where title/description contains Gomiman tag, or customAppPackage matches ours
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.CALENDAR_ID
            )
            val selection = "(${CalendarContract.Events.TITLE} LIKE ?) OR " +
                    "(${CalendarContract.Events.DESCRIPTION} LIKE ?) OR " +
                    "(${CalendarContract.Events.CUSTOM_APP_PACKAGE} = ?)"
            val selectionArgs = arrayOf(
                "%$CALENDAR_EVENT_TAG%",
                "%$CALENDAR_EVENT_TAG%",
                context.packageName
            )

            val targetEventIds = mutableListOf<Long>()
            val affectedCalendarIds = mutableSetOf<Long>()

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CalendarContract.Events._ID)
                val calIdCol = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val calId = cursor.getLong(calIdCol)
                    targetEventIds.add(id)
                    affectedCalendarIds.add(calId)
                }
            }

            var deletedCount = 0
            for (id in targetEventIds) {
                val eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
                val rows = context.contentResolver.delete(eventUri, null, null)
                if (rows > 0) {
                    deletedCount++
                }
            }

            // Notify content observers that calendar events and instances have changed
            context.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null)
            context.contentResolver.notifyChange(CalendarContract.Instances.CONTENT_URI, null)

            if (triggerSync) {
                // Request immediate sync for accounts hosting the affected calendars
                requestSyncForCalendars(affectedCalendarIds)
            }

            deletedCount
        } catch (e: Exception) {
            android.util.Log.e("CalendarRepository", "resetEvents exception: ${e.message}", e)
            0
        }
    }

    private fun requestSyncForCalendars(calendarIds: Set<Long> = emptySet()) {
        try {
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE
            )
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val typeCol = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)

                val syncedAccounts = mutableSetOf<Pair<String, String>>()
                while (cursor.moveToNext()) {
                    val calId = cursor.getLong(idCol)
                    if (calendarIds.isEmpty() || calendarIds.contains(calId)) {
                        val name = cursor.getString(nameCol)
                        val type = cursor.getString(typeCol)
                        if (!name.isNullOrEmpty() && !type.isNullOrEmpty()) {
                            syncedAccounts.add(name to type)
                        }
                    }
                }

                for ((accountName, accountType) in syncedAccounts) {
                    val account = Account(accountName, accountType)
                    val bundle = Bundle().apply {
                        putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                        putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                    }
                    ContentResolver.requestSync(account, CalendarContract.AUTHORITY, bundle)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarRepository", "requestSyncForCalendars failed: ${e.message}")
        }
    }
}
