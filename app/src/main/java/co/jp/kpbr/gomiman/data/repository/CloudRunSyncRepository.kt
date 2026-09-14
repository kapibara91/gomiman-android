package co.jp.kpbr.gomiman.data.repository

import android.util.Log
import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.model.PushSettingModel
import co.jp.kpbr.gomiman.data.model.UserInfoModel
import co.jp.kpbr.gomiman.data.network.ApiClient
import co.jp.kpbr.gomiman.data.network.CloudRunConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * SyncRepository implementation communicating with Google Cloud Run.
 * All HTTP requests automatically include the 'X-Firebase-AppCheck' header via AppCheckInterceptor.
 */
class CloudRunSyncRepository(
    private val baseUrl: String = CloudRunConfig.DEFAULT_BASE_URL,
    private val deviceIdProvider: () -> String? = { null }
) : SyncRepository {

    companion object {
        private const val TAG = "CloudRunSyncRepo"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val client = ApiClient.okHttpClient
    private val gson = ApiClient.gson

    override suspend fun syncBaseInfo(userInfo: UserInfoModel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bodyMap = mapOf(
                "fcmToken" to userInfo.fcmToken,
                "identifierForVendor" to (userInfo.deviceUniqueId ?: deviceIdProvider()),
                "version" to userInfo.version,
                "buildNumber" to userInfo.buildNumber,
                "timeZoneOffsetInHours" to userInfo.timeZoneOffsetInHours,
                "platform" to userInfo.platform,
                "isPhysicalDevice" to userInfo.isPhysicalDevice,
                "brand" to userInfo.brand,
                "model" to userInfo.model,
                "device" to userInfo.device,
                "name" to userInfo.name,
                "systemVersion" to userInfo.systemVersion
            )

            postJson(CloudRunConfig.PATH_SYNC_BASE_INFO, bodyMap)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to syncBaseInfo to Cloud Run", e)
            Result.failure(e)
        }
    }

    override suspend fun syncPushSetting(pushSetting: PushSettingModel): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bodyMap = mapOf(
                "identifierForVendor" to deviceIdProvider(),
                "collectionDayBefore" to pushSetting.collectionDayBefore,
                "collectionDayAfter" to pushSetting.collectionDayAfter,
                "collectionTimeDayBefore" to pushSetting.getDayBeforeHour(),
                "collectionTimeDayAfter" to pushSetting.getDayAfterHour()
            )

            postJson(CloudRunConfig.PATH_SYNC_PUSH_SETTING, bodyMap)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to syncPushSetting to Cloud Run", e)
            Result.failure(e)
        }
    }

    override suspend fun syncGarbageSetting(collections: List<GarbageCollectionModel>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bodyMap = mapOf(
                "identifierForVendor" to deviceIdProvider(),
                "userGarbageInfo" to collections.map { it.toMap() }
            )

            postJson(CloudRunConfig.PATH_SYNC_GARBAGE_SETTING, bodyMap)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to syncGarbageSetting to Cloud Run", e)
            Result.failure(e)
        }
    }

    override suspend fun submitFeedback(message: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bodyMap = mapOf(
                "feedbackMessage" to message,
                "platform" to "Android"
            )

            postJson(CloudRunConfig.PATH_FEEDBACK_SUBMIT, bodyMap)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to submitFeedback to Cloud Run", e)
            Result.failure(e)
        }
    }

    override suspend fun testPing(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = baseUrl.trimEnd('/') + CloudRunConfig.PATH_PING
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Log.d(TAG, "Ping successful (${response.code}): $body")
                    Result.success(body)
                } else {
                    val errMsg = "HTTP ${response.code} ${response.message}: $body"
                    Log.w(TAG, "Ping failed: $errMsg")
                    Result.failure(RuntimeException(errMsg))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception executing testPing", e)
            Result.failure(e)
        }
    }

    private fun postJson(path: String, data: Any) {
        val jsonString = gson.toJson(data)
        val requestBody = jsonString.toRequestBody(JSON_MEDIA_TYPE)
        val url = baseUrl.trimEnd('/') + path

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                throw RuntimeException("Cloud Run request failed [${response.code}]: $errorBody")
            }
        }
    }
}
