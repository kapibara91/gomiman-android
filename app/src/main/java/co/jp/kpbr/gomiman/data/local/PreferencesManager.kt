package co.jp.kpbr.gomiman.data.local

import android.content.Context
import android.content.SharedPreferences
import co.jp.kpbr.gomiman.data.model.PushSettingModel

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "gomiman_prefs"
        private const val KEY_DAY_BEFORE = "_collectionDayBeforeKey"
        private const val KEY_TIME_DAY_BEFORE = "_selectedTimeDayBeforeKey"
        private const val KEY_DAY_AFTER = "_collectionDayAfterKey"
        private const val KEY_TIME_DAY_AFTER = "_selectedTimeDayAfterKey"
        private const val KEY_FIRST_TIME_ADDED = "_firstTimeAddedGarbageKey"
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
}
