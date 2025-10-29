/*
 * MessageHandlerModule.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;


import android.content.Context;

/**
 * Interface for message handling module
 */
public interface MessageHandlerModule {

    /**
     * Called by core library right after initialization of module
     *
     * @param appContext application context
     */
    void init(Context appContext);

    /**
     * Called when message is received
     *
     * @param message message object
     * @return module should return true if this message was consumed and no further processing required
     */
    boolean handleMessage(Message message);

    /**
     * Called when message is tapped by user
     *
     * @param message message object
     * @return module should return true if this message was consumed and no further processing required
     */
    boolean messageTapped(Message message);

    /**
     * Called in module when application goes to foreground
     */
    void applicationInForeground();

    /**
     * Called on module when cleanup is performed on the core library
     */
    void cleanup();


    /**
     * Called on module when user depersonalize is performed on the core library
     */
    void depersonalize();

    /**
     * Called on module when network availability changes
     */
    void performSyncActions();
}
