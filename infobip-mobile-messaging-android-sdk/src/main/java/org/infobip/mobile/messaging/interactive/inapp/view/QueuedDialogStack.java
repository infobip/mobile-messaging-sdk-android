package org.infobip.mobile.messaging.interactive.inapp.view;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.text.TextUtils;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.inapp.image.DownloadImageTask;
import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppCtx;
import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppNativeCtx;
import org.infobip.mobile.messaging.interactive.inapp.view.ctx.InAppWebCtx;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueuedDialogStack implements DialogStack, InAppCtxVisitor {
    public static final int MAX_IN_APP_QUEUE_SIZE = 1;
    private final Queue<InAppCtx> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void add(InAppCtx ctx) {
        queue.add(ctx);
        if (queue.size() <= MAX_IN_APP_QUEUE_SIZE) {
            show(queue.peek());
        }
    }

    @Override
    public void remove(InAppView view) {
        for (InAppCtx ctx : queue) {
            if (ctx.getInAppView().equals(view)) {
                queue.remove(ctx);
                break;
            }
        }
        show(queue.peek());
    }

    @Override
    public void clear() {
        queue.clear();
    }

    private void show(InAppCtx ctx) {
        if (ctx == null) {
            return;
        }

        ctx.accept(this);
    }

    @SuppressLint("StaticFieldLeak")
    private void downloadImageThenShowDialog(final Message message,
                                             final NotificationCategory category,
                                             final NotificationAction[] actions,
                                             String imageUrl,
                                             final InAppNativeView dialog) {
        new DownloadImageTask() {
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap == null) {
                    dialog.show(message, category, actions);
                } else {
                    dialog.showWithImage(bitmap, message, category, actions);
                }
            }
        }.execute(imageUrl);
    }

    @Override
    public void visit(InAppWebCtx ctx) {
        ctx.show();
    }

    @Override
    public void visit(InAppNativeCtx ctx) {
        if (TextUtils.isEmpty(ctx.getMessage().getContentUrl())) {
            ctx.show();
        } else {
            downloadImageThenShowDialog(ctx.getMessage(), ctx.getCategory(), ctx.getActions(), ctx.getMessage().getContentUrl(), (InAppNativeView) ctx.getInAppView());
        }
    }
}
