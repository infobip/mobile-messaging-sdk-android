package org.infobip.mobile.messaging.chat;

import android.content.Context;

/**
 * Main interface for in-app chat communication
 */
@SuppressWarnings("unused")
public abstract class InAppChat {

    /**
     * Returns instance of chat api
     *
     * @param context android context
     * @return instance of chat api
     */
    public synchronized static InAppChat getInstance(Context context) {
        return InAppChatImpl.getInstance(context);
    }

    /**
     * Activates In-app chat service.
     */
    public abstract void activate();

    /**
     * Creates in-app chat view activity
     *
     * @return chat view object
     * @see InAppChatView#show()
     */
    public abstract InAppChatView inAppChatView();

    /**
     * Sets which activities to start when user taps on chat notification. Last one in array will be shown, others will be put to task stack.
     * <p>Library will also provide appropriate message together with intent, use following code to retrieve the message:</p>
     * * <pre>
     * {@code
     * Message message = Message.createFrom(intent);
     * }
     * </pre>
     *
     * @param activityClasses array of activities to put into task stack when message is tapped
     */
    public abstract void setActivitiesToStartOnMessageTap(Class... activityClasses);

    /**
     * Cleans up all InAppChat data.
     * <p>NOTE: There is no need to invoke this method manually as library manages web view data</p>
     */
    public abstract void cleanup();
}
