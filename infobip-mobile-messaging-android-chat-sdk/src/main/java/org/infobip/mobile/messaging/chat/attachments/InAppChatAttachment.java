package org.infobip.mobile.messaging.chat.attachments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.google.android.gms.common.util.JsonUtils.escapeString;

public class InAppChatAttachment {
    public static final long DEFAULT_MAX_UPLOAD_CONTENT_SIZE = 10_485_760; //10 MiB

    String base64;
    String mimeType;
    String fileName;

    public InAppChatAttachment(String mimeType, String base64, String filename) {
        this.base64 = base64;
        this.mimeType = mimeType;
        this.fileName = filename;
    }

    public static InAppChatAttachment makeAttachment(Context context, Intent data) throws InternalSdkError.InternalSdkException {
        byte[] bytesArray = getBytes(context, data);

        if (bytesArray == null) {
            return null;
        }

        if (bytesArray.length > getAttachmentMaxSize(context)) {
            throw InternalSdkError.ERROR_ATTACHMENT_MAX_SIZE_EXCEEDED.getException();
        }

        String mimeType = getMimeType(context, data);
        String encodedString = Base64.encodeToString(bytesArray, Base64.DEFAULT);

        String fileName;
        Uri uri = data.getData();
        if (uri != null) {
            fileName = uri.getLastPathSegment();
        } else {
            fileName = UUID.randomUUID().toString();
        }

        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String extension = mime.getExtensionFromMimeType(mimeType);
        if (extension != null) {
            fileName += "." + extension;
        }

        if (encodedString != null && mimeType != null) {
            return new InAppChatAttachment(mimeType, encodedString, fileName);
        }
        return null;
    }

    public String base64UrlString() {
        return "data:" + mimeType + ";base64," + escapeString(base64);
    }

    private static String getMimeType(Context context, Intent data) {
        String mimeType = "application/octet-stream";
        Uri uri = data.getData();
        if (uri != null) {
            mimeType = data.resolveType(context.getContentResolver());
        } else if (data.getExtras() != null) {
            mimeType = "image/jpeg";
        }
        return mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    private static byte[] getBytes(Context context, Intent data) {
        Uri uri = data.getData();
        if (uri != null) {
            try {
                InputStream stream = context.getContentResolver().openInputStream(uri);
                if (stream == null) {
                    MobileMessagingLogger.e("[InAppChat] Can't get base64 from Uri");
                    return null;
                }
                byte[] bytes = getBytes(stream);
                stream.close();
                return bytes;
            } catch (Exception e) {
                MobileMessagingLogger.e("[InAppChat] Can't get base64 from Uri", e);
                return null;
            }
        } else if (data.getExtras() != null) {
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            if (imageBitmap == null) {
                MobileMessagingLogger.e("[InAppChat] imageBitmap is null");
                return null;
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            return outputStream.toByteArray();
        }
        return null;
    }

    private static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    private static Long getAttachmentMaxSize(Context context) {
        return PreferenceHelper.findLong(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE.getKey(), DEFAULT_MAX_UPLOAD_CONTENT_SIZE);
    }
}