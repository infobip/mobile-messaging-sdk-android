package org.infobip.mobile.messaging.interactive.inapp.cache;

import org.infobip.mobile.messaging.Message;

/**
 * @author sslavin
 * @since 18/04/2018.
 */
public interface OneMessageCache {
    void save(Message message);
    void remove(Message message);
    Message getAndRemove();
}
