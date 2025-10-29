/*
 * InAppCtxVisitor.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.view;

import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppNativeCtx;
import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppWebCtx;

public interface InAppCtxVisitor {
    void visit(InAppWebCtx ctx);
    void visit(InAppNativeCtx ctx);
}

