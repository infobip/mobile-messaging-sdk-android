package org.infobip.mobile.messaging.interactive.inapp.view;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.text.TextUtils;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;
import org.infobip.mobile.messaging.interactive.inapp.image.DownloadImageTask;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author sslavin
 * @since 25/04/2018.
 */
public class QueuedDialogStack implements DialogStack {

    private final Queue<InAppViewCtx> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void add(InAppView view, Message message, NotificationCategory category, NotificationAction[] actions) {
        queue.add(new InAppViewCtx(view, message, category, actions));
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

        if (TextUtils.isEmpty(ctx.getMessage().getContentUrl())) {
            ctx.getInAppView().show(ctx.getMessage(), ctx.getCategory(), ctx.getActions());
        } else {
            downloadImageThenShowDialog(ctx.getMessage(), ctx.getCategory(), ctx.getActions(), ctx.getMessage().getContentUrl(), ctx.getInAppView());
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void downloadImageThenShowDialog(final Message message,
                                             final NotificationCategory category,
                                             final NotificationAction[] actions,
                                             String imageUrl,
                                             final InAppView dialog) {
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
}
