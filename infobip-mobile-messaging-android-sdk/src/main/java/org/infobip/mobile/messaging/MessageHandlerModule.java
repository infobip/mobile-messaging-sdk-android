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
     * Called in module when application goes to foreground
     */
    void applicationInForeground();

    /**
     * Called on module when cleanup is performed on the core library
     */
    void cleanup();


    /**
     * Called on module when user logout is performed on the core library
     */
    void logoutUser();
}
