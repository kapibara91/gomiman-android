package co.jp.kpbr.gomiman.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import co.jp.kpbr.gomiman.GomimanApp
import co.jp.kpbr.gomiman.MainActivity
import co.jp.kpbr.gomiman.R
import co.jp.kpbr.gomiman.data.repository.FirestoreSyncRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch

class GomimanFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token received: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        GomimanApp.instance.applicationScope.launch {
            try {
                val deviceId = GomimanApp.instance.deviceIdProvider() ?: return@launch
                Log.d(TAG, "Syncing refreshed FCM token for device: $deviceId")
                val updateData = mapOf(
                    "fcmToken" to token,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                FirebaseFirestore.getInstance()
                    .collection(FirestoreSyncRepository.COLLECTION_USERS)
                    .document(deviceId)
                    .set(updateData, SetOptions.merge())
                Log.d(TAG, "FCM token synced to Firestore successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync refreshed FCM token to Firestore", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        // When app is in the foreground, FCM does not display notification banners automatically.
        // We construct a local notification using NotificationCompat.
        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["message"]

        if (!body.isNullOrBlank()) {
            showNotification(title, body)
        }
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, GomimanApp.CHANNEL_ID_GARBAGE_REMINDER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(this)
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied while posting notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display notification", e)
        }
    }

    companion object {
        private const val TAG = "GomimanFCMService"
    }
}
