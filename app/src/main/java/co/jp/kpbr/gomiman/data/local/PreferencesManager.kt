package co.jp.kpbr.gomiman.data.local

import android.content.Context
import android.content.SharedPreferences
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.model.PushSettingModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val collectionListType = object : TypeToken<List<GarbageCollectionModel>>() {}.type

    companion object {
        private const val PREFS_NAME = "gomiman_prefs"
        private const val KEY_DAY_BEFORE = "_collectionDayBeforeKey"
        private const val KEY_TIME_DAY_BEFORE = "_selectedTimeDayBeforeKey"
        private const val KEY_DAY_AFTER = "_collectionDayAfterKey"
        private const val KEY_TIME_DAY_AFTER = "_selectedTimeDayAfterKey"
        private const val KEY_FIRST_TIME_ADDED = "_firstTimeAddedGarbageKey"

        // Garbage collection schedule local storage keys (no DB needed)
        private const val KEY_GARBAGE_COLLECTIONS_JSON = "_garbageCollectionsJsonKey"
        private const val KEY_GARBAGE_IS_SYNCED = "_garbageIsSyncedKey"
        private const val KEY_GARBAGE_SCHEDULE_VERSION = "_garbageScheduleVersionKey"
    }

    fun getPushSetting(): PushSettingModel {
        val dayBefore = prefs.getBoolean(KEY_DAY_BEFORE, true)
        val timeDayBefore = prefs.getInt(KEY_TIME_DAY_BEFORE, 1)
        val dayAfter = prefs.getBoolean(KEY_DAY_AFTER, false)
        val timeDayAfter = prefs.getInt(KEY_TIME_DAY_AFTER, 0)
        return PushSettingModel(
            collectionDayBefore = dayBefore,
            selectedTimeDayBefore = timeDayBefore,
            collectionDayAfter = dayAfter,
            selectedTimeDayAfter = timeDayAfter
        )
    }

    fun savePushSetting(setting: PushSettingModel) {
        prefs.edit()
            .putBoolean(KEY_DAY_BEFORE, setting.collectionDayBefore)
            .putInt(KEY_TIME_DAY_BEFORE, setting.selectedTimeDayBefore)
            .putBoolean(KEY_DAY_AFTER, setting.collectionDayAfter)
            .putInt(KEY_TIME_DAY_AFTER, setting.selectedTimeDayAfter)
            .apply()
    }

    fun isFirstTimeAddedGarbage(): Boolean {
        return !prefs.contains(KEY_FIRST_TIME_ADDED)
    }

    fun markFirstTimeAddedGarbage() {
        prefs.edit().putString(KEY_FIRST_TIME_ADDED, System.currentTimeMillis().toString()).apply()
    }

    // --- Garbage Collection Local Persistence ---

    fun getGarbageCollections(): List<GarbageCollectionModel> {
        val json = prefs.getString(KEY_GARBAGE_COLLECTIONS_JSON, null)
        if (json.isNullOrBlank()) return emptyList()
        return try {
            gson.fromJson<List<GarbageCollectionModel>>(json, collectionListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveGarbageCollections(collections: List<GarbageCollectionModel>) {
        val json = gson.toJson(collections)
        prefs.edit().putString(KEY_GARBAGE_COLLECTIONS_JSON, json).apply()
    }

    fun isGarbageSettingSynced(): Boolean {
        return prefs.getBoolean(KEY_GARBAGE_IS_SYNCED, false)
    }

    fun setGarbageSettingSynced(synced: Boolean) {
        prefs.edit().putBoolean(KEY_GARBAGE_IS_SYNCED, synced).apply()
    }

    fun getGarbageScheduleVersion(): Long {
        return prefs.getLong(KEY_GARBAGE_SCHEDULE_VERSION, 0L)
    }

    fun setGarbageScheduleVersion(version: Long) {
        prefs.edit().putLong(KEY_GARBAGE_SCHEDULE_VERSION, version).apply()
    }

    fun updateGarbageScheduleVersion(): Long {
        val newVersion = System.currentTimeMillis()
        setGarbageScheduleVersion(newVersion)
        return newVersion
    }
}
