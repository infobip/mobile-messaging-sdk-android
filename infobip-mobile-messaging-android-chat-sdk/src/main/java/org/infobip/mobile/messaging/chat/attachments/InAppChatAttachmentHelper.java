package org.infobip.mobile.messaging.chat.attachments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import org.infobip.mobile.messaging.mobileapi.InternalSdkError;

public class InAppChatAttachmentHelper {
    public static void makeAttachment(final Activity context, final Intent data, final Uri capturedImageUri, final InAppChatAttachmentHelper.InAppChatAttachmentHelperListener listener) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final InAppChatMobileAttachment attachment = InAppChatMobileAttachment.makeAttachment(context, data, capturedImageUri);
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
        void onAttachmentCreated(InAppChatMobileAttachment attachment);
        void onError(Context context, InternalSdkError.InternalSdkException exception);
    }
}
