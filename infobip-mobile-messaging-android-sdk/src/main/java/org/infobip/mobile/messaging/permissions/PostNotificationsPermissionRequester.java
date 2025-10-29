/*
 * PostNotificationsPermissionRequester.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.permissions;

import android.Manifest;
import android.os.Build;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.resources.R;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;

public class PostNotificationsPermissionRequester implements PermissionsRequestManager.PermissionsRequester {
    private PermissionsRequestManager permissionsRequestManager;
    private Boolean postNotificationPermissionAsked = false;
    private Boolean isRequestEnabled;

    public PostNotificationsPermissionRequester(Boolean isRequestEnabled) {
        this.isRequestEnabled = isRequestEnabled;
    }

    public void onActivityCreated(ComponentActivity activity) {
        if (permissionsRequestManager == null) {
            permissionsRequestManager = new PermissionsRequestManager(activity, this);
        }
    }

    public void onActivityStarted() {
        if (!postNotificationPermissionAsked &&
                isRequestEnabled &&
                permissionsRequestManager != null) {
            permissionsRequestManager.isRequiredPermissionsGranted();
            postNotificationPermissionAsked = true;
        }
    }

    public void onActivityDestroyed() {
        if (permissionsRequestManager != null) {
            permissionsRequestManager = null;
        }
    }

    public void requestPermission() {
        if (permissionsRequestManager != null) {
            permissionsRequestManager.isRequiredPermissionsGranted();
        } else {
            //Activity doesn't yet created, so it's not postponed technically
            isRequestEnabled = true;
        }
    }

    @Override
    public void onPermissionGranted() {
        MobileMessagingLogger.i("POST_NOTIFICATION permission is granted");
    }

    @NonNull
    @Override
    public String[] requiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.POST_NOTIFICATIONS};
        }
        return new String[0];
    }

    @Override
    public boolean shouldShowPermissionsNotGrantedDialogIfShownOnce() {
        return true;
    }

    @Override
    public int permissionsNotGrantedDialogTitle() {
        return R.string.mm_post_notifications_settings_title;
    }

    @Override
    public int permissionsNotGrantedDialogMessage() {
        return R.string.mm_post_notifications_settings_message;
    }
}