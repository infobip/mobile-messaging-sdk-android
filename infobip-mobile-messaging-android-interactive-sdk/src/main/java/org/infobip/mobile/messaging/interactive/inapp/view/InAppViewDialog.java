package org.infobip.mobile.messaging.interactive.inapp.view;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.R;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 13/04/2018.
 */
public class InAppViewDialog implements InAppView {

    private final Callback callback;
    private final View dialogView;
    private final TextView tvMessageText;
    private final RelativeLayout rlDialogImage;
    private final ProgressBar pbDialog;
    private final ImageView image;
    private final Executor uiThreadExecutor;
    private final ActivityWrapper activityWrapper;

    InAppViewDialog(Callback callback, Executor uiThreadExecutor, ActivityWrapper activityWrapper) {
        this.dialogView = activityWrapper.inflateView(R.layout.in_app_dialog_image);
        this.tvMessageText = dialogView.findViewById(R.id.tv_msg_text);
        this.rlDialogImage = dialogView.findViewById(R.id.rl_dialog_image);
        this.pbDialog = dialogView.findViewById(R.id.pb_dialog);
        this.image = dialogView.findViewById(R.id.iv_dialog);
        this.callback = callback;
        this.uiThreadExecutor = uiThreadExecutor;
        this.activityWrapper = activityWrapper;
    }

    @Override
    public void show(@NonNull final Message message, @NonNull final NotificationAction... actions) {
        if (actions.length == 0) {
            return;
        }

        uiThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                showOnUiThread(message, actions);
            }
        });
    }

    private void showOnUiThread(@NonNull Message message, @NonNull NotificationAction[] actions) {
        tvMessageText.setText(message.getBody());
        if (!TextUtils.isEmpty(message.getContentUrl())) {
            rlDialogImage.setVisibility(View.VISIBLE);
            pbDialog.setVisibility(View.VISIBLE);
            image.setVisibility(View.VISIBLE);
        }

        final AlertDialog.Builder builder = activityWrapper.createAlertDialogBuilder()
                .setOnDismissListener(new InAppViewDialogDismissListener(this, callback))
                .setView(dialogView);

        switch (actions.length) {
            case 1:
                builder.setPositiveButton(actions[0].getTitleResourceId(), new InAppViewDialogClickListener(this, callback, message, actions[0]));
                break;
            case 2:
                builder.setNegativeButton(actions[0].getTitleResourceId(), new InAppViewDialogClickListener(this, callback, message, actions[0]))
                        .setPositiveButton(actions[1].getTitleResourceId(), new InAppViewDialogClickListener(this, callback, message, actions[1]));
                break;
            case 3:
            default:
                builder.setNegativeButton(actions[0].getTitleResourceId(), new InAppViewDialogClickListener(this, callback, message, actions[0]))
                        .setNeutralButton(actions[1].getTitleResourceId(), new InAppViewDialogClickListener(this, callback, message, actions[1]))
                        .setPositiveButton(actions[2].getTitleResourceId(), new InAppViewDialogClickListener(this, callback, message, actions[2]));
                break;
        }

        builder.create()
                .show();
    }

    @Override
    public void showImage(@NonNull final Bitmap bitmap) {
        uiThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                pbDialog.setVisibility(View.GONE);
                image.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    public void imageDownloadFailed() {
        uiThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                pbDialog.setVisibility(View.GONE);
            }
        });
    }
}
