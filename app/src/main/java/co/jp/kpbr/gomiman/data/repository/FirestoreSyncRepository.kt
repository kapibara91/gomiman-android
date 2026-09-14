package co.jp.kpbr.gomiman.data.repository

import android.util.Log
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
    private val deviceIdProvider: () -> String? = { null }
) : SyncRepository {

    companion object {
        private const val TAG = "FirestoreSyncRepo"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_FEEDBACKS = "feedbacks"
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

    override suspend fun syncGarbageSetting(collections: List<GarbageCollectionModel>): Result<Unit> {
        // Reserved for future Firestore sync when user specifies schema
        Log.d(TAG, "syncGarbageSetting called with ${collections.size} items (waiting for schema definition)")
        return Result.success(Unit)
    }

    override suspend fun submitFeedback(message: String): Result<Unit> {
        return try {
            val docId = deviceIdProvider() ?: "anonymous"
            val feedbackData = mapOf(
                "deviceUniqueId" to docId,
                "message" to message,
                "platform" to "Android",
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection(COLLECTION_FEEDBACKS)
                .add(feedbackData)
                .await()
            Log.d(TAG, "Feedback submitted to Firestore successfully")
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
