package org.infobip.mobile.messaging.interactive.inapp.view.ctx;

import org.infobip.mobile.messaging.interactive.inapp.view.InAppCtxVisitor;
import org.infobip.mobile.messaging.interactive.inapp.view.InAppView;

public interface InAppCtx {
    InAppView getInAppView();
    void accept(InAppCtxVisitor visitor);
}
