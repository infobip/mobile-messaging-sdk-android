/*
 * InAppCtx.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.view.ctx;

import org.infobip.mobile.messaging.interactive.inapp.view.InAppCtxVisitor;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppView;

public interface InAppCtx {
    InAppView getInAppView();
    void accept(InAppCtxVisitor visitor);
}
