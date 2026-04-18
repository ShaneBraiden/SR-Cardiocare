/**
 * Firebase Cloud Functions — SR-Cardiocare
 *
 * Responsibility: fan out every `notifications/{id}` document write as an
 * FCM data-only message to each of the recipient user's registered device
 * tokens (`users/{uid}.fcmTokens`).
 *
 * The Android client writes notification docs via `core.push.Notifier`,
 * which carries `title`, `body`, `type`, `route` and `params`. We forward
 * all of those as the `data` payload so the client can build its own
 * PendingIntent (tap-to-route) and post its own NotificationCompat. We
 * never send a `notification:` block — that would cause the system
 * launcher to handle taps in background state and bypass our routing.
 */

const {setGlobalOptions} = require("firebase-functions");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

setGlobalOptions({maxInstances: 10});

exports.fanOutNotification = onDocumentCreated(
    "notifications/{id}",
    async (event) => {
      const snapshot = event.data;
      if (!snapshot) {
        logger.warn("fanOutNotification: no snapshot");
        return;
      }
      const doc = snapshot.data() || {};
      const userId = doc.userId;
      if (!userId) {
        logger.warn("fanOutNotification: missing userId",
            {id: event.params.id});
        return;
      }

      // Look up the recipient's registered device tokens.
      const userRef = admin.firestore().collection("users").doc(userId);
      const userSnap = await userRef.get();
      let tokens = [];
      if (userSnap.exists) {
        const raw = userSnap.get("fcmTokens");
        if (Array.isArray(raw)) {
          tokens = raw.filter(
              (t) => typeof t === "string" && t.length > 0,
          );
        }
      }
      if (tokens.length === 0) {
        logger.info("fanOutNotification: no tokens for user", {userId});
        return;
      }

      // Serialise params as JSON so the client can round-trip it through the
      // PendingIntent extras (FCM data values must all be strings).
      const paramsJson = JSON.stringify(doc.params || {});
      const data = {
        title: String(doc.title || ""),
        body: String(doc.body || ""),
        type: String(doc.type || ""),
        route: String(doc.route || ""),
        params: paramsJson,
        channelId: channelFor(doc.type),
        notificationId: String(event.params.id),
      };

      const response = await admin.messaging().sendEachForMulticast({
        tokens,
        data,
        android: {
          priority: "high",
        },
      });

      // Prune tokens that FCM rejected as unregistered/invalid so the user's
      // array doesn't grow stale.
      const invalid = [];
      response.responses.forEach((r, i) => {
        if (r.success) return;
        const code = r.error && r.error.code;
        if (code === "messaging/registration-token-not-registered" ||
            code === "messaging/invalid-registration-token" ||
            code === "messaging/invalid-argument") {
          invalid.push(tokens[i]);
        }
      });
      if (invalid.length > 0) {
        await admin.firestore().collection("users").doc(userId).update({
          fcmTokens: admin.firestore.FieldValue.arrayRemove(...invalid),
        });
        logger.info("fanOutNotification: pruned invalid tokens",
            {userId, count: invalid.length});
      }

      logger.info("fanOutNotification: sent", {
        userId,
        delivered: response.successCount,
        failed: response.failureCount,
      });
    },
);

/**
 * Returns the FCM channel ID for a given notification type.
 * @param {string} type - The notification type.
 * @return {string} The channel ID.
 */
function channelFor(type) {
  switch ((type || "").toLowerCase()) {
    case "message":
      return "srcc_chat";
    case "appointment":
    case "appointment_update":
    case "appointment_request":
      return "srcc_appointments";
    default:
      return "srcc_general";
  }
}
