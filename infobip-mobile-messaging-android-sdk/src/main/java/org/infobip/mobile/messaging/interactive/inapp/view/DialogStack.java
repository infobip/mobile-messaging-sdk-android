package org.infobip.mobile.messaging.interactive.inapp.view;

import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppCtx;

public interface DialogStack {
    void add(InAppCtx ctx);
    void remove(InAppView view);
    void clear();
}
