package skw.auto.test.ext

/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.service.voice.VoiceInteractionSession
import androidx.annotation.StringDef

/**
 * An active voice interaction session on the car, providing additional actions which assistant
 * should act on. Override the [.onShow] to received the action specified
 * by the voice session initiator.
 */
abstract class CarVoiceInteractionSession : VoiceInteractionSession {
    /**
     * The list of exceptions the active voice service must handle.
     */
    @StringDef(EXCEPTION_NOTIFICATION_LISTENER_PERMISSIONS_MISSING)
    annotation class ExceptionValue

    private val mNotificationPayloadHandler: NotificationPayloadHandler

    constructor(context: Context?) : super(context) {
        mNotificationPayloadHandler = NotificationPayloadHandler(getContext())
    }

    constructor(context: Context?, handler: Handler?) : super(context, handler) {
        mNotificationPayloadHandler = NotificationPayloadHandler(getContext())
    }

    /**
     * Returns the notification payload handler, which can be used to handle actions related to
     * notification payloads.
     */
    val notificationPayloadHandler: NotificationPayloadHandler
        get() = mNotificationPayloadHandler

    override fun onShow(args: Bundle, showFlags: Int) {
        super.onShow(args, showFlags)
        if (args != null && isCarNotificationSource(showFlags)) {
            val action = getRequestedVoiceAction(args)
            if (VOICE_ACTION_NO_ACTION != action) {
                onShow(action, args, showFlags)
                return
            }
        }
        onShow(VOICE_ACTION_NO_ACTION, args, showFlags)
    }

    /**
     * Called when the session UI is going to be shown.  This is called after
     * [.onCreateContentView] (if the session's content UI needed to be created) and
     * immediately prior to the window being shown.  This may be called while the window
     * is already shown, if a show request has come in while it is shown, to allow you to
     * update the UI to match the new show arguments.
     *
     * @param action The action that is being requested for this session
     * (e.g. [CarVoiceInteractionSession.VOICE_ACTION_READ_NOTIFICATION],
     * [CarVoiceInteractionSession.VOICE_ACTION_REPLY_NOTIFICATION]).
     * @param args The arguments that were supplied to
     * [VoiceInteractionService.showSession].
     * @param flags The show flags originally provided to
     * [VoiceInteractionService.showSession].
     */
    protected abstract fun onShow(action: String?, args: Bundle?, flags: Int)

    companion object {
        /** The key used for the action [String] in the payload [Bundle].  */
        const val KEY_ACTION = "KEY_ACTION"

        /**
         * The key used for the [CarVoiceInteractionSession.VOICE_ACTION_HANDLE_EXCEPTION] payload
         * [Bundle]. Must map to a [ExceptionValue].
         */
        const val KEY_EXCEPTION = "KEY_EXCEPTION"

        /**
         * The key used for the payload [Bundle], if a [StatusBarNotification] is used as
         * the payload.
         */
        const val KEY_NOTIFICATION = "KEY_NOTIFICATION"

        /** Indicates to assistant that no action was specified.  */
        const val VOICE_ACTION_NO_ACTION = "VOICE_ACTION_NO_ACTION"

        /** Indicates to assistant that a read action is being requested for a given payload.  */
        const val VOICE_ACTION_READ_NOTIFICATION = "VOICE_ACTION_READ_NOTIFICATION"

        /** Indicates to assistant that a reply action is being requested for a given payload.  */
        const val VOICE_ACTION_REPLY_NOTIFICATION = "VOICE_ACTION_REPLY_NOTIFICATION"

        /**
         * Indicates to assistant that it should resolve the exception in the given payload (found in
         * [CarVoiceInteractionSession.KEY_EXCEPTION]'s value).
         */
        const val VOICE_ACTION_HANDLE_EXCEPTION = "VOICE_ACTION_HANDLE_EXCEPTION"

        /**
         * Indicates to assistant that it is missing the Notification Listener permission, and should
         * request this permission from the user.
         */
        const val EXCEPTION_NOTIFICATION_LISTENER_PERMISSIONS_MISSING =
            "EXCEPTION_NOTIFICATION_LISTENER_PERMISSIONS_MISSING"

        /**
         * Returns true if the request was initiated for a car notification.
         */
        private fun isCarNotificationSource(flags: Int): Boolean {
            return flags and SHOW_SOURCE_NOTIFICATION != 0
        }

        /**
         * Returns the action [String] provided in the args {@Bundle},
         * or [CarVoiceInteractionSession.VOICE_ACTION_NO_ACTION] if no such string was provided.
         */
        protected fun getRequestedVoiceAction(args: Bundle): String {
            return args.getString(KEY_ACTION, VOICE_ACTION_NO_ACTION)
        }
    }
}