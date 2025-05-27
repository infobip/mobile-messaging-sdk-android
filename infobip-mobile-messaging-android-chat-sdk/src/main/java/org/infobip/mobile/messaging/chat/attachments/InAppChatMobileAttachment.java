package org.infobip.mobile.messaging.chat.attachments;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import org.infobip.mobile.messaging.api.chat.WidgetAttachmentConfig;
import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.utils.CommonUtils;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import androidx.exifinterface.media.ExifInterface;

public class InAppChatMobileAttachment {

    /**
     * Default max size for attachment upload.
     * Deprecated, attachment max size it now configurable per Livechat Widget, use {@link WidgetInfo}'s {@link WidgetAttachmentConfig} property {@code maxSize} instead.
     */
    @Deprecated
    public static final long DEFAULT_MAX_UPLOAD_CONTENT_SIZE = 10_485_760; //10 MiB

    String base64;
    String mimeType;
    String fileName;

    public InAppChatMobileAttachment(String mimeType, String base64, String filename) {
        this.base64 = base64;
        this.mimeType = mimeType;
        this.fileName = filename;
    }

    /**
     * Creates {@link InAppChatMobileAttachment} from provided data.
     *
     * @param context               context
     * @param data                  intent with data
     * @param capturedMediaStoreUri media store uri
     * @param capturedMediaRealUri  real uri
     * @return InAppChatMobileAttachment
     * @throws InternalSdkError.InternalSdkException if attachment is not valid or exceeds max size {@link #DEFAULT_MAX_UPLOAD_CONTENT_SIZE}
     * @deprecated use {@link #makeAttachment(Context, Uri)} instead
     */
    @Deprecated
    public static InAppChatMobileAttachment makeAttachment(Context context, Intent data, Uri capturedMediaStoreUri, Uri capturedMediaRealUri) throws InternalSdkError.InternalSdkException {
        String mimeType = getMimeType(context, data, capturedMediaRealUri);
        byte[] bytesArray = getBytes(context, data, capturedMediaStoreUri, capturedMediaRealUri, mimeType);

        if (bytesArray == null) {
            return null;
        }

        if (bytesArray.length > getAttachmentMaxSize(context)) {
            throw InternalSdkError.ERROR_ATTACHMENT_MAX_SIZE_EXCEEDED.getException();
        }
        if (bytesArray.length <= 0) {
            throw InternalSdkError.ERROR_ATTACHMENT_NOT_VALID.getException();
        }

        String encodedString = Base64.encodeToString(bytesArray, Base64.DEFAULT);
        Uri uri = (data != null && data.getData() != null) ? data.getData() : capturedMediaRealUri;
        String fileName = requireFileName(context, uri, mimeType);

        if (encodedString != null && mimeType != null) {
            return new InAppChatMobileAttachment(mimeType, encodedString, fileName);
        }
        return null;
    }

    /**
     * Creates {@link InAppChatMobileAttachment} from provided {@link Uri}.
     *
     * @param context context
     * @param uri     attachment file's uri
     * @return {@link InAppChatMobileAttachment} you can sent in in-app chat
     * @throws IllegalStateException if attachment is not valid or exceeds max size {@link #DEFAULT_MAX_UPLOAD_CONTENT_SIZE}
     */
    public static InAppChatMobileAttachment makeAttachment(Context context, Uri uri) throws IllegalStateException {
        return AttachmentHelper.createInAppChatAttachment(context, uri, (int) DEFAULT_MAX_UPLOAD_CONTENT_SIZE);
    }

    public String base64UrlString() {
        return "data:" + mimeType + ";base64," + CommonUtils.escapeJsonString(base64);
    }

    public String getBase64() {
        return base64;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isValid() {
        return StringUtils.isNotBlank(getFileName()) && StringUtils.isNotBlank(getBase64()) && StringUtils.isNotBlank(getMimeType());
    }

    @Deprecated
    public static String getMimeType(Context context, Intent data, Uri capturedMediaUri) {
        String mimeType = "application/octet-stream";
        if (data != null && data.getData() != null) {
            mimeType = data.resolveType(context.getContentResolver());
        } else if (capturedMediaUri != null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(capturedMediaUri.getPath());
            if (extension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }
        return mimeType;
    }

    private static byte[] getBytes(Context context, Intent data, Uri capturedMediaStoreUri, Uri capturedMediaRealUri, String mimeType) {
        Uri uriFromIntent = null;

        //data.getData() will be null for images captured by camera
        if (data != null && data.getData() != null) {
            uriFromIntent = data.getData();
        }
        //Ony images captured by camera are scaled for now
        if (mimeType.equals("image/jpeg") && uriFromIntent == null) {
            return getBytesWithBitmapScaling(context, capturedMediaStoreUri, capturedMediaRealUri);
        } else if (uriFromIntent != null) {
            return getBytes(context, uriFromIntent);
        } else {
            return getBytes(context, capturedMediaRealUri);
        }
    }

    private static byte[] getBytesWithBitmapScaling(Context context, Uri mediaStoreUri, Uri imageUri) {
        if (context == null || mediaStoreUri == null || imageUri == null) {
            return null;
        }

        try {
            ParcelFileDescriptor fileDescriptor = context.getContentResolver().openFileDescriptor(mediaStoreUri, "r");

            long fileSize = fileDescriptor.getStatSize();
            String filePath = imageUri.getPath();

            boolean shouldScaleImage = fileSize > getAttachmentMaxSize(context);
            int imageQuality = fileSize > DEFAULT_MAX_UPLOAD_CONTENT_SIZE / 2 ? 80 : 100;

            BitmapFactory.Options options = new BitmapFactory.Options();
            //  setting inSampleSize value allows to load a scaled down version of the original image
            options.inSampleSize = shouldScaleImage ? 2 : 1;
            // set to false to load the actual bitmap
            options.inJustDecodeBounds = false;
            //  this options allow android to claim the bitmap memory if it runs low on memory
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inTempStorage = new byte[16 * 1024];

            Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), new Rect(-1, -1, -1, -1), options);

            //  check the rotation of the image and display it properly
            int orientationDegree = getExifOrientationDegree(context, filePath, mediaStoreUri);
            Matrix rotationMatrix = new Matrix();
            rotationMatrix.postRotate(orientationDegree);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotationMatrix, true);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, imageQuality, outputStream);
            bitmap.recycle();
            fileDescriptor.close();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            MobileMessagingLogger.e("[InAppChat] can't load image to send attachment", exception);
            return null;
        }
    }

    private static int getExifOrientationDegree(Context context, String filepath, Uri mediaStoreUri) {
        int degree = 0;
        ExifInterface exif = null;
        InputStream inputStream = null;
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                inputStream = context.getContentResolver().openInputStream(mediaStoreUri);
                exif = new ExifInterface(inputStream);
            } else {
                exif = new ExifInterface(filepath);
            }
        } catch (IOException exception) {
            MobileMessagingLogger.e("[InAppChat] can't get image orientation", exception);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
        if (exif != null) {
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation != -1) {
                // We only recognise a subset of orientation tag values.
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        degree = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        degree = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        degree = 270;
                        break;
                }

            }
        }

        return degree;
    }

    private static byte[] getBytes(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        ParcelFileDescriptor fileDescriptor = null;
        try {
            InputStream stream = null;
            if (Build.VERSION.SDK_INT >= 24) {
                fileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r", null);
                stream = new FileInputStream(fileDescriptor.getFileDescriptor());
            } else {
                stream = context.getContentResolver().openInputStream(uri);
            }
            if (stream == null) {
                MobileMessagingLogger.e("[InAppChat] Can't get base64 from Uri");
                return null;
            }
            byte[] bytes = getBytes(stream);
            stream.close();
            if (fileDescriptor != null) {
                fileDescriptor.close();
            }
            return bytes;
        } catch (Exception e) {
            MobileMessagingLogger.e("[InAppChat] Can't get base64 from Uri", e);
            return null;
        }
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
        return PreferenceHelper.findLong(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ATTACHMENT_MAX_SIZE.getKey(), DEFAULT_MAX_UPLOAD_CONTENT_SIZE);
    }

    /**
     * Uses ContentResolver to query real file name for provided uri.
     * @param context context
     * @param uri file's uri
     * @return real file name if success, null otherwise
     */
    @Nullable
    private static String queryFileName(Context context, Uri uri) {
        ContentResolver contentResolver = context.getApplicationContext().getContentResolver();
        if (contentResolver != null) {
            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                String name = cursor.getString(nameIndex);
                cursor.close();
                return name;
            }
        }
        return null;
    }

    /**
     * Returns file name for provided uri. Value for file name is resolved from multiple source-by-source priority.
     * The source with the highest priority defines a final file name value. If source does not provide file name value,
     * there is fallback to the source with lower priority.
     * Sources:
     * 1. Real file name from ContentResolver
     * 2. Last path segment from Uri
     * 3. Generated random UUID
     * @param context context
     * @param uri file's uri
     * @param mimeType file's mime type
     * @return file name
     */
    @NotNull
    private static String requireFileName(Context context, Uri uri, String mimeType) {
        String fileName = null;
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (uri != null) {
            fileName = queryFileName(context, uri);
            if (StringUtils.isNotBlank(fileName))
                return fileName;
            fileName = uri.getLastPathSegment();
        }
        if (StringUtils.isBlank(fileName)){
            fileName = UUID.randomUUID().toString();
        }
        if (extension != null) {
            fileName += "." + extension;
        }
        return fileName;
    }

    @Override
    public String toString() {
        return "InAppChatMobileAttachment{" +
                "base64='" + base64 + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}