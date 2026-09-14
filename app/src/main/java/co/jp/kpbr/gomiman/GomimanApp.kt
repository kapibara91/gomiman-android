package co.jp.kpbr.gomiman

import android.app.Application
import android.provider.Settings
import co.jp.kpbr.gomiman.data.local.GarbageDatabaseHelper
import co.jp.kpbr.gomiman.data.local.PreferencesManager
import co.jp.kpbr.gomiman.data.model.UserInfoModel
import co.jp.kpbr.gomiman.data.network.AppCheckManager
import co.jp.kpbr.gomiman.data.repository.CalendarRepository
import co.jp.kpbr.gomiman.data.repository.CloudRunSyncRepository
import co.jp.kpbr.gomiman.data.repository.GarbageRepository
import co.jp.kpbr.gomiman.data.repository.SyncRepository
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
        syncRepository = CloudRunSyncRepository(deviceIdProvider = deviceIdProvider)

        try {
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            // Ads initialization safe fallback
        }

        applicationScope.launch {
            garbageRepository.loadGarbageCollections()

            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            val userInfo = UserInfoModel(
                deviceUniqueId = deviceId,
                brand = android.os.Build.BRAND,
                model = android.os.Build.MODEL,
                device = android.os.Build.DEVICE,
                systemVersion = "${android.os.Build.VERSION.SDK_INT}"
            )
//            syncRepository.syncBaseInfo(userInfo)

            syncRepository.testPing()
        }
    }

    companion object {
        lateinit var instance: GomimanApp
            private set
    }
}
