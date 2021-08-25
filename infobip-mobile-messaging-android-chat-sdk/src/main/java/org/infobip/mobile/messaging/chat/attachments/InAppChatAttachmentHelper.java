package org.infobip.mobile.messaging.chat.attachments;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentActivity;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.SoftwareInformation;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class InAppChatAttachmentHelper {
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String OUTPUT_MEDIA_PATH = "/InAppChat";

    public static void makeAttachment(final FragmentActivity context, final Intent data, final Uri mediaStoreUri, final InAppChatAttachmentHelper.InAppChatAttachmentHelperListener listener) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Uri imageUri = getUriFromMediaStoreURI(mediaStoreUri, context);
                    ParcelFileDescriptor fileDescriptor = openFileDescriptorFromMediaStoreURI(mediaStoreUri, context);
                    final InAppChatMobileAttachment attachment = InAppChatMobileAttachment.makeAttachment(context, data, mediaStoreUri, imageUri, fileDescriptor);
                    closeFileDescriptor(fileDescriptor);
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

    @Nullable
    public static Uri getOutputMediaUri(FragmentActivity fragmentActivity) {
        if (fragmentActivity == null) {
            return null;
        }
        String appName = SoftwareInformation.getAppName(fragmentActivity.getApplicationContext());
        File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File outputPicturesDirectory = new File(picturesDirectory.getPath() + File.separator + appName + OUTPUT_MEDIA_PATH);
        if (!outputPicturesDirectory.exists() && !outputPicturesDirectory.mkdirs()) {
            MobileMessagingLogger.e("[InAppChat]", "Can't create directory for temporary saving attachment");
            return null;
        }

        String fileName = JPEG_FILE_PREFIX + DateTimeUtil.dateToYMDHMSString(new Date()) + JPEG_FILE_SUFFIX;
        File pictureFile = new File(outputPicturesDirectory.getPath() + File.separator + fileName);

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, pictureFile.getPath());
        return fragmentActivity.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    @RequiresApi(29)
    @Nullable
    public static Uri getOutputMediaUrlAPI29(FragmentActivity fragmentActivity) {
        if (fragmentActivity == null) {
            return null;
        }
        String fileName = JPEG_FILE_PREFIX + DateTimeUtil.dateToYMDHMSString(new Date()) + JPEG_FILE_SUFFIX;
        String appName = SoftwareInformation.getAppName(fragmentActivity.getApplicationContext());
        String mimeType = "image/jpeg";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

        //in case if mm_compileSdkVersion > 28, it will be MediaStore.MediaColumns.RELATIVE_PATH
        contentValues.put("relative_path", Environment.DIRECTORY_PICTURES + File.separator + appName + OUTPUT_MEDIA_PATH);

        return fragmentActivity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    @Nullable
    private static Uri getUriFromMediaStoreURI(Uri mediaStoreUri, FragmentActivity activity) {
        if (activity == null || mediaStoreUri == null) {
            return null;
        }

        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.managedQuery(mediaStoreUri, proj, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return Uri.fromFile(new File(cursor.getString(column_index)));
    }

    @Nullable
    private static ParcelFileDescriptor openFileDescriptorFromMediaStoreURI(Uri mediaStoreUri, FragmentActivity activity) {
        if (activity == null || mediaStoreUri == null) {
            return null;
        }

        ParcelFileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = activity.getApplicationContext().getContentResolver().openFileDescriptor(mediaStoreUri, "rw");
        } catch (Exception e) {
            MobileMessagingLogger.e("[InAppChat] Can't send attachment", e);
        }
        return fileDescriptor;
    }

    private static void closeFileDescriptor(ParcelFileDescriptor fileDescriptor) {
        if (fileDescriptor == null) {
            return;
        }
        try {
            fileDescriptor.close();
        } catch (IOException e) {
            MobileMessagingLogger.e("[InAppChat] Can't close file descriptor", e);
        }
    }

    public interface InAppChatAttachmentHelperListener {
        void onAttachmentCreated(InAppChatMobileAttachment attachment);
        void onError(Context context, InternalSdkError.InternalSdkException exception);
    }
}
