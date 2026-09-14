package co.jp.kpbr.gomiman.data.model

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
)
