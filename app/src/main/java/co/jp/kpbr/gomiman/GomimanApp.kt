package co.jp.kpbr.gomiman

import android.app.Application
import android.provider.Settings
import android.util.Log
import co.jp.kpbr.gomiman.data.local.GarbageDatabaseHelper
import co.jp.kpbr.gomiman.data.local.PreferencesManager
import co.jp.kpbr.gomiman.data.network.AppCheckManager
import co.jp.kpbr.gomiman.data.repository.CalendarRepository
import co.jp.kpbr.gomiman.data.repository.FirestoreSyncRepository
import co.jp.kpbr.gomiman.data.repository.GarbageRepository
import co.jp.kpbr.gomiman.data.repository.SyncRepository
import co.jp.kpbr.gomiman.utils.UserInfoCollector
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

class GomimanApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val deviceIdProvider: () -> String? = {
        try {
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }
    }

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

        // Initialize Notification Channel (Android 8.0+ / API 26+)
        createNotificationChannel()

        preferencesManager = PreferencesManager(this)
        databaseHelper = GarbageDatabaseHelper(this)
        garbageRepository = GarbageRepository(preferencesManager = preferencesManager, dbHelper = databaseHelper)
        calendarRepository = CalendarRepository(this)
        syncRepository = FirestoreSyncRepository(deviceIdProvider = deviceIdProvider)

        val gmsAvailability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (gmsAvailability == ConnectionResult.SUCCESS) {
            try {
                MobileAds.initialize(this) {}
            } catch (e: SecurityException) {
                Log.w("GomimanApp", "SecurityException initializing MobileAds (GMS broker unavailable)", e)
            } catch (e: Exception) {
                Log.w("GomimanApp", "Failed to initialize MobileAds", e)
            }
        } else {
            Log.w("GomimanApp", "Google Play Services unavailable (code=$gmsAvailability). Skipping MobileAds init.")
        }

        applicationScope.launch {
            garbageRepository.loadGarbageCollections()

            // If local schedules have not synced to server yet, attempt sync on startup
            if (!garbageRepository.isSynced.value && garbageRepository.garbageModels.value.isNotEmpty()) {
                val syncRes = garbageRepository.syncWithServer(syncRepository)
                if (syncRes.isSuccess) {
                    Log.d("GomimanApp", "Pending garbage schedules synced to server on launch")
                }
            }

            // Retrieve FCM token if available
            val fcmToken = try {
                val token = FirebaseMessaging.getInstance().token.await()
                Log.d("GomimanApp", "Obtained FCM token: $token")
                token
            } catch (e: Exception) {
                Log.w("GomimanApp", "Failed to obtain FCM token on launch", e)
                null
            }

            // Collect device and system info with fcmToken, then sync to Firestore on app open
            val userInfo = UserInfoCollector.collect(this@GomimanApp, fcmToken = fcmToken)
            val syncResult = syncRepository.syncBaseInfo(userInfo)
            if (syncResult.isSuccess) {
                Log.d("GomimanApp", "User info successfully synced to Firestore on launch")
            } else {
                Log.w("GomimanApp", "Failed to sync user info on launch", syncResult.exceptionOrNull())
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_GARBAGE_REMINDER,
                CHANNEL_NAME_GARBAGE_REMINDER,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "ごみの収集日や前日のリマインド通知"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            Log.d("GomimanApp", "Notification channel '$CHANNEL_ID_GARBAGE_REMINDER' created")
        }
    }

    companion object {
        lateinit var instance: GomimanApp
            private set

        const val CHANNEL_ID_GARBAGE_REMINDER = "gomiman_garbage_reminder"
        const val CHANNEL_NAME_GARBAGE_REMINDER = "ゴミ収集日のお知らせ"
    }
}
