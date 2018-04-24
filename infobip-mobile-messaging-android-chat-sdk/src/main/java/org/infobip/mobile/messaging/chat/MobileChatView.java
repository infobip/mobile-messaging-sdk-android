package org.infobip.mobile.messaging.chat;

import org.infobip.mobile.messaging.interactive.NotificationCategory;

/**
 * @author sslavin
 * @since 19/11/2017.
 */

public interface MobileChatView {

    /**
     * Will register action categories in chat view. All messages containing these categories will also have buttons for corresponding actions.
     * @param categories categories to register
     * @return view
     */
    MobileChatView withActionCategories(NotificationCategory... categories);

    /**
     * Call this method to show the view
     */
    void show();
}
