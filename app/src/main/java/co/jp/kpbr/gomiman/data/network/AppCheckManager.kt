package co.jp.kpbr.gomiman.data.network

import android.content.Context
import android.util.Log
import co.jp.kpbr.gomiman.BuildConfig
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.tasks.await

/**
 * Manages Firebase App Check initialization and token retrieval.
 *
 * In Debug builds or when Google Play Services is unavailable,
 * DebugAppCheckProviderFactory is used as fallback.
 *
 * In Release builds, PlayIntegrityAppCheckProviderFactory is used when
 * Google Play Services is available on the device.
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

                val availability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                val isGmsAvailable = (availability == ConnectionResult.SUCCESS)

                if (BuildConfig.DEBUG || !isGmsAvailable) {
                    Log.d(
                        TAG,
                        "Initializing Firebase App Check with DebugAppCheckProviderFactory " +
                                "(DEBUG=${BuildConfig.DEBUG}, gmsAvailable=$isGmsAvailable, gmsCode=$availability)"
                    )
                    firebaseAppCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                } else {
                    Log.d(TAG, "Initializing Firebase App Check with PlayIntegrityAppCheckProviderFactory")
                    try {
                        firebaseAppCheck.installAppCheckProviderFactory(
                            PlayIntegrityAppCheckProviderFactory.getInstance()
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to install PlayIntegrityAppCheckProviderFactory, falling back to Debug provider", e)
                        firebaseAppCheck.installAppCheckProviderFactory(
                            DebugAppCheckProviderFactory.getInstance()
                        )
                    }
                }

                isInitialized = true
                Log.i(TAG, "Firebase App Check initialized successfully")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException while initializing Firebase App Check (GMS broker unavailable)", e)
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
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException obtaining App Check token: ${e.message}")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to obtain App Check token: ${e.message}")
            null
        }
    }
}
