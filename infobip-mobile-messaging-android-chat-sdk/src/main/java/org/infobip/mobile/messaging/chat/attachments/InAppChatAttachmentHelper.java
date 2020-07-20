package org.infobip.mobile.messaging.chat.attachments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.infobip.mobile.messaging.mobileapi.InternalSdkError;

public class InAppChatAttachmentHelper {
    public static void makeAttachment(final Activity context, final Intent data, final InAppChatAttachmentHelper.InAppChatAttachmentHelperListener listener) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final InAppChatAttachment attachment = InAppChatAttachment.makeAttachment(context, data);
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onAttachmentCreated(attachment);
                        }
                    });
                } catch (final InternalSdkError.InternalSdkException e) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(context, e);
                        }
                    });
                }
            }
        });
    }

    public interface InAppChatAttachmentHelperListener {
        void onAttachmentCreated(InAppChatAttachment attachment);
        void onError(Context context, InternalSdkError.InternalSdkException exception);
    }
}
