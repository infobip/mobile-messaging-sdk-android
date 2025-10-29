/*
 * OneMessageCache.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
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
