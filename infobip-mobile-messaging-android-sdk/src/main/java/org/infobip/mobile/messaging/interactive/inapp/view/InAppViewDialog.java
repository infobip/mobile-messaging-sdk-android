/*
 * InAppViewDialog.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.view;

import android.app.Activity;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.R;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

/**
 * @author sslavin
 * @since 13/04/2018.
 */
public class InAppViewDialog implements InAppNativeView {
    private final Callback callback;
    private final View dialogView;
    private final TextView tvMessageText;
    private final TextView tvMessageTitle;
    private final RelativeLayout rlDialogImage;
    private final ImageView image;
    private final Executor uiThreadExecutor;
    private final ActivityWrapper activityWrapper;

    InAppViewDialog(Callback callback, Executor uiThreadExecutor, ActivityWrapper activityWrapper) {
        this.dialogView = activityWrapper.inflateView(R.layout.in_app_dialog_image);
        this.tvMessageText = dialogView.findViewById(R.id.tv_msg_text);
        this.tvMessageTitle = dialogView.findViewById(R.id.tv_msg_title);
        this.rlDialogImage = dialogView.findViewById(R.id.rl_dialog_image);
        this.image = dialogView.findViewById(R.id.iv_dialog);
        this.callback = callback;
        this.uiThreadExecutor = uiThreadExecutor;
        this.activityWrapper = activityWrapper;
    }

    @Override
    public void show(@NonNull final Message message, @NonNull final NotificationCategory category, @NonNull final NotificationAction... actions) {
        show(null, message, category, actions);
    }

    @Override
    public void showWithImage(@NonNull final Bitmap bitmap, @NonNull final Message message, final NotificationCategory category, @NonNull final NotificationAction... actions) {
        show(bitmap, message, category, actions);
    }

    private void show(final Bitmap bitmap, @NonNull final Message message, final NotificationCategory category, @NonNull final NotificationAction... actions) {
        if (actions.length == 0) {
            return;
        }
        uiThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    showOnUiThread(bitmap, message, category, actions);
                } catch (Exception e) {
                    if (e instanceof IllegalStateException && e.getMessage().contains("You need to use a Theme.AppCompat theme")) {
                        return;
                    }
                    // better not displaying in-app than crashing UI
                    MobileMessagingLogger.e("Failed to build in-app view due to " + e.getMessage(), e);
                }
            }
        });
    }

    private void showOnUiThread(Bitmap bitmap, @NonNull Message message, NotificationCategory category, @NonNull NotificationAction[] actions) {
        if (bitmap != null) {
            image.setImageBitmap(bitmap);
            rlDialogImage.setVisibility(View.VISIBLE);
            image.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(message.getTitle())) {
            tvMessageTitle.setText(message.getTitle());
            tvMessageTitle.setVisibility(View.VISIBLE);
        }
        tvMessageText.setText(message.getBody());

        final AlertDialog.Builder builder = activityWrapper.createAlertDialogBuilder()
                .setOnDismissListener(new InAppViewDialogDismissListener(this, callback))
                .setView(dialogView);

        switch (actions.length) {
            case 1:
                builder.setPositiveButton(getPositiveButtonText(message, 0, actions), new InAppViewDialogClickListener(this, callback, message, category, actions[0]));
                break;
            case 2:
                builder.setNegativeButton(getNegativeButtonText(message, 0, actions), new InAppViewDialogClickListener(this, callback, message, category, actions[0]))
                        .setPositiveButton(getPositiveButtonText(message, 1, actions), new InAppViewDialogClickListener(this, callback, message, category, actions[1]));
                break;
            case 3:
            default:
                builder.setNegativeButton(getNegativeButtonText(message, 0, actions), new InAppViewDialogClickListener(this, callback, message, category, actions[0]))
                        .setNeutralButton(actions[1].getTitleResourceId(), new InAppViewDialogClickListener(this, callback, message, category, actions[1]))
                        .setPositiveButton(getPositiveButtonText(message, 2, actions), new InAppViewDialogClickListener(this, callback, message, category, actions[2]));
                break;
        }

        builder.create()
                .show();
    }

    private String getPositiveButtonText(Message message, int i, @NonNull NotificationAction[] actions) {
        if (StringUtils.isNotBlank(message.getInAppOpenTitle())) {
            return message.getInAppOpenTitle();
        }
        Activity activity = activityWrapper.getActivity();
        if (activity != null) {
            return activity.getResources().getString(actions[i].getTitleResourceId());
        }
        // this should never happen as we have activity context when in-app is being built
        return "";
    }

    private String getNegativeButtonText(Message message, int i, @NonNull NotificationAction[] actions) {
        if (StringUtils.isNotBlank(message.getInAppDismissTitle())) {
            return message.getInAppDismissTitle();
        }
        Activity activity = activityWrapper.getActivity();
        if (activity != null) {
            return activity.getResources().getString(actions[i].getTitleResourceId());
        }
        // this should never happen as we have activity context when in-app is being built
        return "";
    }
}
