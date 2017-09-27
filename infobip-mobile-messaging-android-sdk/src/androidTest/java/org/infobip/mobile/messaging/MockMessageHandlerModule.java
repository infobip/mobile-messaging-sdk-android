package org.infobip.mobile.messaging;

import android.content.Context;

/**
 * @author tjuric
 * @since 19/09/17.
 */

public class MockMessageHandlerModule implements MessageHandlerModule {

    @Override
    public void setContext(Context appContext) {

    }

    @Override
    public void messageReceived(Message message) {

    }

    @Override
    public void applicationInForeground() {

    }

    @Override
    public void cleanup() {

    }
}
