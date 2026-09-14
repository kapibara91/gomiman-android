package co.jp.kpbr.gomiman

import co.jp.kpbr.gomiman.data.model.GarbageCollectionModel
import co.jp.kpbr.gomiman.data.model.PushSettingModel
import co.jp.kpbr.gomiman.data.model.UserInfoModel
import co.jp.kpbr.gomiman.data.network.ApiClient
import co.jp.kpbr.gomiman.data.network.CloudRunConfig
import co.jp.kpbr.gomiman.data.repository.CloudRunSyncRepositoryStub
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudRunNetworkTest {

    @Test
    fun testCloudRunConfigEndpoints() {
        assertEquals("X-Firebase-AppCheck", CloudRunConfig.APP_CHECK_HEADER)
        assertEquals("/healthz", CloudRunConfig.PATH_HEALTH)
        assertEquals("/api/v1/ping", CloudRunConfig.PATH_PING)
        assertEquals("/user/sync-user/base-info", CloudRunConfig.PATH_SYNC_BASE_INFO)
        assertEquals("/user/sync-user/push-setting", CloudRunConfig.PATH_SYNC_PUSH_SETTING)
        assertEquals("/user/sync-user/garbage-setting", CloudRunConfig.PATH_SYNC_GARBAGE_SETTING)
        assertEquals("/feedback/submit", CloudRunConfig.PATH_FEEDBACK_SUBMIT)
    }

    @Test
    fun testPushSettingPayloadMapping() {
        val pushSetting = PushSettingModel(
            collectionDayBefore = true,
            selectedTimeDayBefore = 2, // 21:00
            collectionDayAfter = true,
            selectedTimeDayAfter = 3  // 08:00
        )
        assertEquals(21, pushSetting.getDayBeforeHour())
        assertEquals(8, pushSetting.getDayAfterHour())
    }

    @Test
    fun testGarbageCollectionModelToMap() {
        val collection = GarbageCollectionModel(
            id = 1L,
            weekStatus = GarbageCollectionModel.WEEK_STATUS_EVERY_WEEK,
            weeks = mutableListOf(1, 2),
            garbageTypes = mutableListOf(1, 3),
            days = mutableListOf(2, 5)
        )
        val map = collection.toMap()
        assertEquals(GarbageCollectionModel.WEEK_STATUS_EVERY_WEEK, map["weekStatus"])
        assertEquals(listOf(1, 2), map["weeks"])
        assertEquals(listOf(1, 3), map["garbageTypes"])
        assertEquals(listOf(2, 5), map["days"])

        val json = ApiClient.gson.toJson(map)
        assertTrue(json.contains("\"weekStatus\":1"))
        assertTrue(json.contains("\"garbageTypes\":[1,3]"))
    }

    @Test
    fun testUserInfoSerialization() {
        val user = UserInfoModel(
            deviceUniqueId = "test-device-uuid-123",
            fcmToken = "token-xyz",
            brand = "Google",
            model = "Pixel 8",
            device = "shiba",
            systemVersion = "35"
        )
        val json = ApiClient.gson.toJson(user)
        assertTrue(json.contains("\"deviceUniqueId\":\"test-device-uuid-123\""))
        assertTrue(json.contains("\"model\":\"Pixel 8\""))
    }

    @Test
    fun testStubPing() = runBlocking {
        val stub = CloudRunSyncRepositoryStub()
        val pingResult = stub.testPing()
        assertTrue(pingResult.isSuccess)
        assertNotNull(pingResult.getOrNull())
    }
}
