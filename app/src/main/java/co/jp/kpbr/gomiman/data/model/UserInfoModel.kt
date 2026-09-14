package co.jp.kpbr.gomiman.data.model

import com.google.firebase.firestore.FieldValue

data class UserInfoModel(
    var deviceUniqueId: String? = null,
    var fcmToken: String? = null,
    var version: String = "1.4.3",
    var buildNumber: String = "7",
    var timeZoneOffsetInHours: Long = 9,
    var platform: String = "Android",
    var isPhysicalDevice: Boolean = true,
    var brand: String = "",
    var model: String = "",
    var device: String = "",
    var name: String = "",
    var systemVersion: String = ""
) {
    fun toMap(includeServerTimestamp: Boolean = true): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "deviceUniqueId" to deviceUniqueId,
            "version" to version,
            "buildNumber" to buildNumber,
            "timeZoneOffsetInHours" to timeZoneOffsetInHours,
            "platform" to platform,
            "isPhysicalDevice" to isPhysicalDevice,
            "brand" to brand,
            "model" to model,
            "device" to device,
            "name" to name,
            "systemVersion" to systemVersion
        )
        if (includeServerTimestamp) {
            map["updatedAt"] = FieldValue.serverTimestamp()
        }
        if (fcmToken != null) {
            map["fcmToken"] = fcmToken
        }
        return map
    }
}

