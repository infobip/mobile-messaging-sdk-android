package org.infobip.mobile.messaging.interactive.inapp.view;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.text.TextUtils;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.inapp.DownloadImageTask;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author sslavin
 * @since 25/04/2018.
 */
public class QueuedDialogStack implements DialogStack {

    private final Queue<InAppViewCtx> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void add(InAppView view, Message message, NotificationAction[] actions) {
        queue.add(new InAppViewCtx(view, message, actions));
        if (queue.size() <= 1) {
            show(queue.peek());
        }
    }

    @Override
    public void remove(InAppView view) {
        for (InAppViewCtx ctx : queue) {
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

    private void show(InAppViewCtx ctx) {
        if (ctx == null) {
            return;
        }

        ctx.getInAppView().show(ctx.getMessage(), ctx.getActions());
        if (!TextUtils.isEmpty(ctx.getMessage().getContentUrl())) {
            downloadImage(ctx.getMessage().getContentUrl(), ctx.getInAppView());
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void downloadImage(String imageUrl, final InAppView dialog) {
        new DownloadImageTask() {
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap == null) {
                    dialog.imageDownloadFailed();
                } else {
                    dialog.showImage(bitmap);
                }
            }
        }.execute(imageUrl);
    }
}
