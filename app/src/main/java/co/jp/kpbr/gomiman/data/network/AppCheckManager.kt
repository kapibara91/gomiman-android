package co.jp.kpbr.gomiman.data.network

import android.content.Context
import android.util.Log
import co.jp.kpbr.gomiman.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.tasks.await

/**
 * Manages Firebase App Check initialization and token retrieval.
 *
 * In Debug builds, DebugAppCheckProviderFactory is used.
 * When running the debug APK, Firebase prints a debug secret to Logcat:
 *   "Enter this debug secret into the allow list in the Firebase Console for your project: XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX"
 *
 * In Release builds, PlayIntegrityAppCheckProviderFactory is used, verifying
 * device authenticity via Google Play Integrity API with the registered release signing key.
 */
object AppCheckManager {
    private const val TAG = "AppCheckManager"

    @Volatile
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        synchronized(this) {
            if (isInitialized) return
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }

                val firebaseAppCheck = FirebaseAppCheck.getInstance()

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Initializing Firebase App Check with DebugAppCheckProviderFactory")
                    firebaseAppCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                } else {
                    Log.d(TAG, "Initializing Firebase App Check with PlayIntegrityAppCheckProviderFactory")
                    firebaseAppCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                }

                isInitialized = true
                Log.i(TAG, "Firebase App Check initialized successfully (DEBUG=${BuildConfig.DEBUG})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Firebase App Check", e)
            }
        }
    }

    /**
     * Retrieve a valid Firebase App Check token.
     *
     * @param forceRefresh If true, forces a token refresh even if the cached token is valid.
     * @return The token string, or null if retrieval failed.
     */
    suspend fun getAppCheckToken(forceRefresh: Boolean = false): String? {
        return try {
            val appCheck = FirebaseAppCheck.getInstance()
            val result = appCheck.getAppCheckToken(forceRefresh).await()
            val token = result.token
            Log.d(TAG, "Obtained App Check token: length=${token.length}, expireTimeMillis=${result.expireTimeMillis}")
            token
        } catch (e: Exception) {
            Log.w(TAG, "Failed to obtain App Check token: ${e.message}")
            null
        }
    }
}
