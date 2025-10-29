/*
 * InAppViewDialogDismissListener.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.view;

import android.content.DialogInterface;

/**
 * @author sslavin
 * @since 25/04/2018.
 */
public class InAppViewDialogDismissListener implements DialogInterface.OnDismissListener {

    private final InAppView inAppView;
    private final InAppView.Callback callback;

    InAppViewDialogDismissListener(InAppView inAppView, InAppView.Callback callback) {
        this.inAppView = inAppView;
        this.callback = callback;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        callback.dismissed(inAppView);
    }
}
