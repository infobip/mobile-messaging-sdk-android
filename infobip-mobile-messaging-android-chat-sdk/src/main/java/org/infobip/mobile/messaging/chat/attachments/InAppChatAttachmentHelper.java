package org.infobip.mobile.messaging.chat.attachments;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.common.MMAsyncTask;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.SoftwareInformation;

import java.io.File;
import java.util.Date;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

public class InAppChatAttachmentHelper {
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String OUTPUT_MEDIA_PATH = "/InAppChat";
    private static final String VIDEO_FILE_PREFIX = "VIDEO_";
    public static final String MIME_TYPE_VIDEO_MP_4 = "video/mp4";
    public static final String MIME_TYPE_IMAGE_JPEG = "image/jpeg";

    public static void makeAttachment(final FragmentActivity context, final Intent data, final Uri capturedMediaStoreUri, final InAppChatAttachmentHelper.InAppChatAttachmentHelperListener listener) {
        MMAsyncTask.execute(() -> {
            try {
                //From media store Uri we need to get real Uri of the file
                Uri capturedMediaRealUri = getUriFromMediaStoreURI(capturedMediaStoreUri, context);
                final InAppChatMobileAttachment attachment = InAppChatMobileAttachment.makeAttachment(context, data, capturedMediaStoreUri, capturedMediaRealUri);
                context.runOnUiThread(() -> listener.onAttachmentCreated(attachment));
            } catch (final InternalSdkError.InternalSdkException e) {
                context.runOnUiThread(() -> listener.onError(context, e));
            }
        });
    }

    @Nullable
    public static Uri getOutputImageUri(FragmentActivity fragmentActivity) {
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
    public static Uri getOutputImageUrlAPI29(FragmentActivity fragmentActivity) {
        if (fragmentActivity == null) {
            return null;
        }
        String fileName = JPEG_FILE_PREFIX + DateTimeUtil.dateToYMDHMSString(new Date()) + JPEG_FILE_SUFFIX;
        String appName = SoftwareInformation.getAppName(fragmentActivity.getApplicationContext());

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE_IMAGE_JPEG);

        contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + File.separator + appName + OUTPUT_MEDIA_PATH
        );

        return fragmentActivity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    @RequiresApi(31)
    @Nullable
    // Required only for 31 API, to get rid of camera error on emulator "Only owner is able to interact with pending item"
    public static Uri getOutputVideoUrl(FragmentActivity fragmentActivity) {
        if (fragmentActivity == null) {
            return null;
        }
        String fileName = VIDEO_FILE_PREFIX + DateTimeUtil.dateToYMDHMSString(new Date());
        String appName = SoftwareInformation.getAppName(fragmentActivity.getApplicationContext());

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE_VIDEO_MP_4);

        contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + File.separator + appName + OUTPUT_MEDIA_PATH
        );
        return fragmentActivity.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    public static Boolean isUriFileEmpty(Context context, Uri uri) {
        if (context == null || uri == null)
            return null;
        try {
            ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
            Cursor cursor = contentResolver.query(uri, new String[]{OpenableColumns.SIZE}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (columnIndex == 0) {
                    long fileSize = cursor.getLong(columnIndex);
                    return fileSize <= 0;
                }
                cursor.close();
            }
            return null;
        } catch (Exception e) {
            MobileMessagingLogger.e("[InAppChat] Can't detect file size", e);
            return null;
        }
    }

    public static void deleteEmptyFileByUri(Context context, Uri uri) {
        if (context == null || uri == null)
            return;
        try {
            ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
            Boolean isFileEmpty = isUriFileEmpty(context, uri);
            if (Boolean.TRUE.equals(isFileEmpty)) {
                contentResolver.delete(uri, null, null);
            }
        } catch (Exception e) {
            MobileMessagingLogger.e("[InAppChat] Can't delete empty file", e);
        }
    }

    @Nullable
    private static Uri getUriFromMediaStoreURI(Uri mediaStoreUri, FragmentActivity activity) {
        if (activity == null || mediaStoreUri == null) {
            return null;
        }

        try {
            Uri uri = null;
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = activity.getContentResolver().query(mediaStoreUri, proj, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                if (columnIndex >= 0) {
                    uri = Uri.fromFile(new File(cursor.getString(columnIndex)));
                }
                cursor.close();
            }
            return uri;
        } catch (Throwable t) {
            return null;
        }
    }

    public interface InAppChatAttachmentHelperListener {
        void onAttachmentCreated(InAppChatMobileAttachment attachment);

        void onError(Context context, Exception exception);
    }
}
