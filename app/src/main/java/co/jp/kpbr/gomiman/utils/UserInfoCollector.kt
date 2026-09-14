package co.jp.kpbr.gomiman.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import co.jp.kpbr.gomiman.BuildConfig
import co.jp.kpbr.gomiman.data.model.UserInfoModel
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object UserInfoCollector {

    /**
     * Gathers device hardware and system information to construct a [UserInfoModel].
     */
    fun collect(context: Context, fcmToken: String? = null): UserInfoModel {
        val deviceId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } catch (e: Exception) {
            null
        }

        val timeZoneOffsetHours = try {
            val offsetMillis = TimeZone.getDefault().getOffset(System.currentTimeMillis())
            TimeUnit.MILLISECONDS.toHours(offsetMillis.toLong())
        } catch (e: Exception) {
            9L
        }

        val versionName = try {
            BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            "1.0.0"
        }

        val buildNumber = try {
            BuildConfig.VERSION_CODE.toString()
        } catch (e: Exception) {
            "1"
        }

        val systemVersion = "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val deviceName = if (Build.MODEL.startsWith(Build.MANUFACTURER, ignoreCase = true)) {
            Build.MODEL
        } else {
            "${Build.MANUFACTURER} ${Build.MODEL}"
        }

        return UserInfoModel(
            deviceUniqueId = deviceId,
            fcmToken = fcmToken,
            version = versionName,
            buildNumber = buildNumber,
            timeZoneOffsetInHours = timeZoneOffsetHours,
            platform = "Android",
            isPhysicalDevice = checkIsPhysicalDevice(),
            brand = Build.BRAND.orEmpty(),
            model = Build.MODEL.orEmpty(),
            device = Build.DEVICE.orEmpty(),
            name = deviceName,
            systemVersion = systemVersion
        )
    }

    private fun checkIsPhysicalDevice(): Boolean {
        return !(Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }
}
