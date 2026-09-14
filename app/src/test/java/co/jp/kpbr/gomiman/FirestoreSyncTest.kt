package co.jp.kpbr.gomiman

import co.jp.kpbr.gomiman.data.model.UserInfoModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreSyncTest {

    @Test
    fun testUserInfoModelToMapWithoutServerTimestamp() {
        val userInfo = UserInfoModel(
            deviceUniqueId = "android-test-uuid-999",
            fcmToken = "test-token-abc",
            version = "1.4.3",
            buildNumber = "7",
            timeZoneOffsetInHours = 9,
            platform = "Android",
            isPhysicalDevice = true,
            brand = "Google",
            model = "Pixel 8",
            device = "shiba",
            name = "Google Pixel 8",
            systemVersion = "14 (API 34)"
        )

        val map = userInfo.toMap(includeServerTimestamp = false)

        assertEquals("android-test-uuid-999", map["deviceUniqueId"])
        assertEquals("test-token-abc", map["fcmToken"])
        assertEquals("1.4.3", map["version"])
        assertEquals("7", map["buildNumber"])
        assertEquals(9L, map["timeZoneOffsetInHours"])
        assertEquals("Android", map["platform"])
        assertEquals(true, map["isPhysicalDevice"])
        assertEquals("Google", map["brand"])
        assertEquals("Pixel 8", map["model"])
        assertEquals("shiba", map["device"])
        assertEquals("Google Pixel 8", map["name"])
        assertEquals("14 (API 34)", map["systemVersion"])
        // Without server timestamp, updatedAt should not be in the map
        assertTrue(!map.containsKey("updatedAt"))
    }

    @Test
    fun testUserInfoModelDefaults() {
        val defaultUser = UserInfoModel()
        assertNotNull(defaultUser.version)
        assertNotNull(defaultUser.buildNumber)
        assertEquals("Android", defaultUser.platform)
    }
}
