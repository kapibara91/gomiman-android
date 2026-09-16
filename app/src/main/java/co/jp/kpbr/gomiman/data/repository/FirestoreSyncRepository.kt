package co.jp.kpbr.gomiman.data.repository

import android.os.Build
import android.util.Log
import co.jp.kpbr.gomiman.BuildConfig
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.model.PushSettingModel
import co.jp.kpbr.gomiman.data.model.UserInfoModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * SyncRepository implementation saving data directly to Firebase Cloud Firestore.
 */
class FirestoreSyncRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val deviceIdProvider: () -> String? = { null },
    private val pushSettingProvider: (() -> PushSettingModel)? = null,
    private val cloudRunSyncRepository: CloudRunSyncRepository = CloudRunSyncRepository(
        deviceIdProvider = deviceIdProvider,
        pushSettingProvider = pushSettingProvider
    )
) : SyncRepository {

    companion object {
        private const val TAG = "FirestoreSyncRepo"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_FEEDBACK = "feedback"
        const val COLLECTION_FEEDBACKS = "feedback"
    }

    override suspend fun syncBaseInfo(userInfo: UserInfoModel): Result<Unit> {
        return try {
            val docId = userInfo.deviceUniqueId?.takeIf { it.isNotBlank() }
                ?: deviceIdProvider()?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalArgumentException("Device ID cannot be empty when syncing user info to Firestore."))

            Log.d(TAG, "Syncing user info to Firestore collection '$COLLECTION_USERS' for docId: $docId")
            val userMap = userInfo.toMap(includeServerTimestamp = true)

            firestore.collection(COLLECTION_USERS)
                .document(docId)
                .set(userMap, SetOptions.merge())
                .await()

            Log.d(TAG, "Successfully synced user info to Firestore for docId: $docId")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException syncing user info to Firestore (GMS broker unavailable)", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user info to Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun syncPushSetting(pushSetting: PushSettingModel): Result<Unit> {
        // Reserved for future Firestore sync when user specifies schema
        Log.d(TAG, "syncPushSetting called for Firestore (waiting for schema definition)")
        return Result.success(Unit)
    }

    override suspend fun syncGarbageSetting(collections: List<GarbageCollectionModel>, version: Long): Result<Unit> {
        // 1. Primary: sync via Cloud Run /api/v1/garbage/schedule to save and schedule Cloud Tasks
        val cloudRunResult = cloudRunSyncRepository.syncGarbageSetting(collections, version)
        if (cloudRunResult.isSuccess) {
            Log.d(TAG, "Garbage setting successfully synced to Cloud Run /api/v1/garbage/schedule with version $version")
            return cloudRunResult
        }

        Log.w(TAG, "Cloud Run /api/v1/garbage/schedule failed, falling back to direct Firestore write", cloudRunResult.exceptionOrNull())

        // 2. Fallback: write directly to Firestore so user changes are persisted
        return syncToFirestoreDirectly(collections, version)
    }

    private suspend fun syncToFirestoreDirectly(collections: List<GarbageCollectionModel>, version: Long): Result<Unit> {
        return try {
            val docId = deviceIdProvider()?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalArgumentException("Device ID cannot be empty when syncing garbage setting to Firestore."))

            Log.d(TAG, "Fallback: Syncing ${collections.size} garbage schedules to Firestore for docId: $docId with version: $version")
            val data = mapOf(
                "userGarbageInfo" to collections.map { it.toMap() },
                "version" to version,
                "garbageVersion" to version,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(COLLECTION_USERS)
                .document(docId)
                .set(data, SetOptions.merge())
                .await()

            Log.d(TAG, "Successfully synced garbage setting to Firestore directly with version $version")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException syncing garbage setting to Firestore directly (GMS broker unavailable)", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync garbage setting to Firestore directly", e)
            Result.failure(e)
        }
    }

    override suspend fun submitFeedback(message: String): Result<Unit> {
        return try {
            val docId = deviceIdProvider() ?: "anonymous"
            val appVersion = BuildConfig.VERSION_NAME
            val buildNumber = BuildConfig.VERSION_CODE.toString()
            val deviceModel = if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) {
                Build.MODEL
            } else {
                "${Build.MANUFACTURER} ${Build.MODEL}"
            }
            val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

            val feedbackData = mapOf(
                "deviceUniqueId" to docId,
                "identifierForVendor" to docId,
                "message" to message,
                "feedbackMessage" to message,
                "platform" to "Android",
                "appVersion" to appVersion,
                "buildNumber" to buildNumber,
                "deviceModel" to deviceModel,
                "osVersion" to osVersion,
                "timestamp" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp()
            )

            firestore.collection(COLLECTION_FEEDBACK)
                .add(feedbackData)
                .await()

            Log.d(TAG, "Feedback submitted to Firestore collection '$COLLECTION_FEEDBACK' successfully")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException submitting feedback to Firestore (GMS broker unavailable)", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit feedback to Firestore", e)
            Result.failure(e)
        }
    }

    override suspend fun testPing(): Result<String> {
        return try {
            val appName = firestore.app.name
            Log.d(TAG, "Firestore testPing ok. FirebaseApp: $appName")
            Result.success("Firestore connected: $appName")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException during Firestore testPing (GMS broker unavailable)", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Firestore testPing failed", e)
            Result.failure(e)
        }
    }
}
