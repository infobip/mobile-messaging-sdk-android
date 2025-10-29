/*
 * ActivityWrapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.view;

import android.app.Activity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

import org.infobip.mobile.messaging.R;

import androidx.appcompat.app.AlertDialog;


/**
 * @author sslavin
 * @since 24/04/2018.
 */
public class ActivityWrapper {

    private final Activity activity;
    private final LayoutInflater layoutInflater;

    ActivityWrapper(Activity activity) {
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
    }

    public AlertDialog.Builder createAlertDialogBuilder() {
        return new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.InAppDialog));
    }

    public View inflateView(int layoutResId) {
        return layoutInflater.inflate(layoutResId, null, false);
    }

    public Activity getActivity() {
        return activity;
    }
}
