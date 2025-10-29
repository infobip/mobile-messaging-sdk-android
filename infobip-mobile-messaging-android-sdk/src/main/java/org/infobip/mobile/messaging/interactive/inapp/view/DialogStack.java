/*
 * DialogStack.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.view;

import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppCtx;

public interface DialogStack {
    void add(InAppCtx ctx);
    void remove(InAppView view);
    void clear();
}
