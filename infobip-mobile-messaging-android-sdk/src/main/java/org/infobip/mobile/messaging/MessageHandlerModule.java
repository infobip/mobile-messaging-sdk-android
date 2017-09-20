package org.infobip.mobile.messaging;


import android.content.Context;

public interface MessageHandlerModule {

    void setContext(Context appContext);

    void messageReceived(Message message);

    void applicationInForeground();
}
