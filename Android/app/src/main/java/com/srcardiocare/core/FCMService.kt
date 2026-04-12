// FCMService.kt — Firebase Cloud Messaging token management and push delivery
package com.srcardiocare.core

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.srcardiocare.data.firebase.FirebaseService

/**
 * Handles FCM token lifecycle and incoming push messages.
 *
 * Token lifecycle:
 * - [onNewToken] fires when FCM issues a fresh token (first install or token rotation).
 *   It saves the token to Firestore so Cloud Functions can target this device.
 * - On every login, call [saveFcmToken] to ensure the latest token is persisted
 *   (covers the case where the token was generated before the user logged in).
 *
 * Incoming messages:
 * - [onMessageReceived] fires for data/notification messages while the app is
 *   in the foreground. Background/quit messages are shown automatically by FCM.
 */
class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        // Persist token to Firestore so backend can send targeted notifications
        val uid = FirebaseService.currentUID ?: return
        saveTokenToFirestore(uid, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "SR-Cardiocare"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: return

        val type = message.data["type"] ?: "activity"
        val activityId = message.data["id"] ?: message.messageId ?: "0"

        // Route to the correct notification channel based on type
        when (type) {
            "workout_reminder" -> NotificationService.showWorkoutReminderNotification(
                context = applicationContext,
                workoutName = title,
                minutesBefore = message.data["minutesBefore"]?.toIntOrNull() ?: 0,
                notificationId = activityId.hashCode()
            )
            else -> NotificationService.showActivityNotification(
                context = applicationContext,
                title = title,
                message = body,
                activityId = activityId
            )
        }
    }

    companion object {
        private const val TAG = "FCMService"

        /**
         * Call this after login to ensure the current FCM token is saved for the
         * authenticated user. Safe to call multiple times — Firestore merge is idempotent.
         */
        fun saveFcmToken(uid: String) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                saveTokenToFirestore(uid, token)
            }
        }

        private fun saveTokenToFirestore(uid: String, token: String) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token)
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to save FCM token", e)
                }
        }
    }
}
