package co.jp.kpbr.gomiman

import android.app.Application
import android.provider.Settings
import co.jp.kpbr.gomiman.data.local.GarbageDatabaseHelper
import co.jp.kpbr.gomiman.data.local.PreferencesManager
import co.jp.kpbr.gomiman.data.model.UserInfoModel
import co.jp.kpbr.gomiman.data.network.AppCheckManager
import co.jp.kpbr.gomiman.data.repository.CalendarRepository
import co.jp.kpbr.gomiman.data.repository.FirestoreSyncRepository
import co.jp.kpbr.gomiman.data.repository.GarbageRepository
import co.jp.kpbr.gomiman.data.repository.SyncRepository
import co.jp.kpbr.gomiman.utils.UserInfoCollector
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GomimanApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var databaseHelper: GarbageDatabaseHelper
        private set
    lateinit var garbageRepository: GarbageRepository
        private set
    lateinit var calendarRepository: CalendarRepository
        private set
    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var syncRepository: SyncRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Firebase App Check
        AppCheckManager.initialize(this)

        val deviceIdProvider = {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        }

        databaseHelper = GarbageDatabaseHelper(this)
        garbageRepository = GarbageRepository(databaseHelper)
        calendarRepository = CalendarRepository(this)
        preferencesManager = PreferencesManager(this)
        syncRepository = FirestoreSyncRepository(deviceIdProvider = deviceIdProvider)

        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            // Ads initialization safe fallback
        }

        applicationScope.launch {
            garbageRepository.loadGarbageCollections()

            // Collect device and system info, then sync to Firestore on app open
            val userInfo = UserInfoCollector.collect(this@GomimanApp)
            val syncResult = syncRepository.syncBaseInfo(userInfo)
            if (syncResult.isSuccess) {
                android.util.Log.d("GomimanApp", "User info successfully synced to Firestore on launch")
            } else {
                android.util.Log.w("GomimanApp", "Failed to sync user info on launch", syncResult.exceptionOrNull())
            }
        }
    }

    companion object {
        lateinit var instance: GomimanApp
            private set
    }
}
