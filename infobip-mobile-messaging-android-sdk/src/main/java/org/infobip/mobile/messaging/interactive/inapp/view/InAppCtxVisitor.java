package org.infobip.mobile.messaging.interactive.inapp.view;

import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppNativeCtx;
import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppWebCtx;

public interface InAppCtxVisitor {
    void visit(InAppWebCtx ctx);
    void visit(InAppNativeCtx ctx);
}

