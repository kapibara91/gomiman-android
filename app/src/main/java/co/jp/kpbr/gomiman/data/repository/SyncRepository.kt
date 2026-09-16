package co.jp.kpbr.gomiman.data.repository

import android.util.Log
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.model.PushSettingModel
import co.jp.kpbr.gomiman.data.model.UserInfoModel
import kotlinx.coroutines.delay

/**
 * Repository interface for synchronizing user state and settings.
 * Network implementations will connect to Google Cloud Run API endpoints.
 */
interface SyncRepository {
    suspend fun syncBaseInfo(userInfo: UserInfoModel): Result<Unit>
    suspend fun syncPushSetting(pushSetting: PushSettingModel): Result<Unit>
    suspend fun syncGarbageSetting(collections: List<GarbageCollectionModel>, version: Long = 0L): Result<Unit>
    suspend fun submitFeedback(message: String): Result<Unit>
    suspend fun testPing(): Result<String>
}

/**
 * Stubbed implementation ready to be swapped with Cloud Run REST / gRPC client.
 */
class CloudRunSyncRepositoryStub : SyncRepository {

    companion object {
        private const val TAG = "CloudRunSync"
        // Future Cloud Run Base URL
        const val BASE_URL = "https://gomiman-api-placeholder.a.run.app"
    }

    override suspend fun syncBaseInfo(userInfo: UserInfoModel): Result<Unit> {
        Log.d(TAG, "syncBaseInfo stub called for device: ${userInfo.deviceUniqueId}")
        delay(100)
        return Result.success(Unit)
    }

    override suspend fun syncPushSetting(pushSetting: PushSettingModel): Result<Unit> {
        Log.d(TAG, "syncPushSetting stub called: $pushSetting")
        delay(100)
        return Result.success(Unit)
    }

    override suspend fun syncGarbageSetting(collections: List<GarbageCollectionModel>, version: Long): Result<Unit> {
        Log.d(TAG, "syncGarbageSetting stub called with ${collections.size} schedules, version: $version")
        delay(100)
        return Result.success(Unit)
    }

    override suspend fun submitFeedback(message: String): Result<Unit> {
        Log.d(TAG, "submitFeedback stub called: message length ${message.length}")
        delay(300)
        return Result.success(Unit)
    }

    override suspend fun testPing(): Result<String> {
        Log.d(TAG, "testPing stub called")
        delay(100)
        return Result.success("""{"status":"ok","message":"stub"}""")
    }
}
