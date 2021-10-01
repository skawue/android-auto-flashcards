package skw.auto.test.ext

import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.ArrayList

/**
 * Class used by [CarVoiceInteractionSession] to handle payload actions for
 * [StatusBarNotification] payloads, such as getting payload data, writing to remote inputs,
 * and firing the appropriate actions upon completion.
 */
class NotificationPayloadHandler(
    /** The context used by this instance to fire actions  */
    private val mContext: Context
) {
    /** @return The [StatusBarNotification], or null if not found.
     */
    fun getStatusBarNotification(args: Bundle): StatusBarNotification? {
        return args.getParcelable(CarVoiceInteractionSession.KEY_NOTIFICATION)
    }

    /**
     * Returns the [Notification] of the [StatusBarNotification]
     * provided in the args [Bundle].
     *
     * @return The [StatusBarNotification]'s [Notification], or null if not found.
     */
    fun getNotification(args: Bundle): Notification? {
        val sbn =
            args.getParcelable<StatusBarNotification>(CarVoiceInteractionSession.KEY_NOTIFICATION)
        return sbn?.notification
    }

    /**
     * Retrieves all messages associated with the provided [StatusBarNotification] in the
     * args [Bundle]. These messages are provided through the notification's
     * [MessagingStyle], using [MessagingStyle.addMessage].
     *
     * @param args the payload delivered to the voice interaction session
     * @return all messages provided in the [MessagingStyle]
     */
    fun getMessages(args: Bundle): List<NotificationCompat.MessagingStyle.Message> {
        val notification = getNotification(args)
        val messagingStyle = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification!!)
        return messagingStyle?.messages
            ?: ArrayList()
    }

    /**
     * Retrieves the corresponding [Action] from the notification's callback actions.
     *
     * @param args the payload delivered to the voice interaction session
     * @param semanticAction the [Action.SemanticAction] on which to select
     * @return the first action for which [Action.getSemanticAction] returns semanticAction,
     * or null if no such action exists
     */
    fun getAction(args: Bundle, semanticAction: Int): Notification.Action? {
        val notification = getNotification(args)
        if (notification == null) {
            Log.w(TAG, "getAction args bundle did not contain a notification")
            return null
        }
        for (action in notification.actions) {
            if (action.semanticAction == semanticAction) {
                return action
            }
        }
        Log.w(TAG, String.format("Semantic action not found: %d", semanticAction))
        return null
    }

    /**
     * Fires the [PendingIntent] of the corresponding [Action], ensuring that any
     * [RemoteInput]s corresponding to this action contain any addidional data.
     *
     * @param action the action to fire
     * @return true if the [PendingIntent] was sent successfully; false otherwise.
     */
    fun fireAction(action: Notification.Action, additionalData: Intent?): Boolean {
        val pendingIntent = action.actionIntent
        val resultCode = 0
        try {
            if (additionalData == null) {
                pendingIntent.send(resultCode)
            } else {
                pendingIntent.send(mContext, resultCode, additionalData)
            }
        } catch (e: CanceledException) {
            return false
        }
        return true
    }

    /**
     * Writes the given reply to the [RemoteInput] of the provided action callback.
     * Requires that the action callback contains at least one [RemoteInput].
     * In the case that multiple [RemoteInput]s are provided, the first will be used.
     *
     * @param actionCallback the action containing the [RemoteInput]
     * @param reply the reply that should be written to the [RemoteInput]
     * @return the additional data to provide to the action intent upon firing; null on error
     *
     * @see NotificationPayloadHandler.fireAction
     */
    fun writeReply(actionCallback: Notification.Action?, reply: CharSequence?): Intent? {
        if (actionCallback == null) {
            Log.e(TAG, "No action callback was provided.")
            return null
        }
        val remoteInputs = actionCallback.remoteInputs
        if (remoteInputs == null || remoteInputs.size == 0) {
            Log.e(TAG, "No RemoteInputs were provided in the action callback.")
            return null
        }
        if (remoteInputs.size > 1) {
            Log.w(TAG, "Vague arguments. Using first RemoteInput.")
        }
        val remoteInput = remoteInputs[0]
        if (remoteInput == null) {
            Log.e(TAG, "RemoteInput provided was null.")
            return null
        }
        val additionalData = Intent()
        val results = Bundle()
        results.putCharSequence(remoteInput.resultKey, reply)
        RemoteInput.addResultsToIntent(remoteInputs, additionalData, results)
        return additionalData
    }

    /**
     * Writes the given reply to the [RemoteInput] of the reply callback, if present.
     * Requires that a reply callback be included in the args [Bundle], and that this
     * callback contains at least one [RemoteInput]. In the case that multiple
     * [RemoteInput]s are provided, the first will be used.
     *
     * @param args the payload arguments provided to the session
     * @param reply the reply that should be written to the [RemoteInput]
     * @return the additional data to provide to the reply action intent upon firing; null on error
     */
    fun writeReply(args: Bundle, reply: CharSequence?): Intent? {
        return writeReply(getAction(args, Notification.Action.SEMANTIC_ACTION_REPLY), reply)
    }

    companion object {
        private const val TAG = "NotificationPayloadHandler"
    }
}