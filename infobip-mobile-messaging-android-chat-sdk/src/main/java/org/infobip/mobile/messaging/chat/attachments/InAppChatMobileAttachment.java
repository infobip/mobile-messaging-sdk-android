package org.infobip.mobile.messaging.chat.attachments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.utils.CommonUtils;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.util.PreferenceHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class InAppChatMobileAttachment {
    public static final long DEFAULT_MAX_UPLOAD_CONTENT_SIZE = 10_485_760; //10 MiB
    public static final float DEFAULT_IMAGE_MAX_HEIGHT = 1280.0f;
    public static final float DEFAULT_IMAGE_MAX_WIDTH = 720.0f;

    String base64;
    String mimeType;
    String fileName;

    public InAppChatMobileAttachment(String mimeType, String base64, String filename) {
        this.base64 = base64;
        this.mimeType = mimeType;
        this.fileName = filename;
    }

    public static InAppChatMobileAttachment makeAttachment(Context context, Intent data, Uri capturedImageUri) throws InternalSdkError.InternalSdkException {
        String mimeType = getMimeType(context, data, capturedImageUri);
        byte[] bytesArray = getBytes(context, data, capturedImageUri, mimeType);

        if (bytesArray == null) {
            return null;
        }

        if (bytesArray.length > getAttachmentMaxSize(context)) {
            throw InternalSdkError.ERROR_ATTACHMENT_MAX_SIZE_EXCEEDED.getException();
        }

        String encodedString = Base64.encodeToString(bytesArray, Base64.DEFAULT);
        Uri uri = (data != null && data.getData() != null) ? data.getData() : capturedImageUri;
        String fileName = (uri != null) ? uri.getLastPathSegment() : UUID.randomUUID().toString();

        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String extension = mime.getExtensionFromMimeType(mimeType);
        if (extension != null) {
            fileName += "." + extension;
        }

        if (encodedString != null && mimeType != null) {
            return new InAppChatMobileAttachment(mimeType, encodedString, fileName);
        }
        return null;
    }

    public String base64UrlString() {
        return "data:" + mimeType + ";base64," + CommonUtils.escapeJsonString(base64);
    }

    public String getFileName() {
        return fileName;
    }

    private static String getMimeType(Context context, Intent data, Uri capturedImageUri) {
        String mimeType = "application/octet-stream";
        if (data != null && data.getData() != null) {
            mimeType = data.resolveType(context.getContentResolver());
        } else if (capturedImageUri != null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(capturedImageUri.getPath());
            if (extension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }
        return mimeType;
    }

    private static byte[] getBytes(Context context, Intent data, Uri capturedImageUri, String mimeType) {
        Uri uri = capturedImageUri;
        if (data != null && data.getData() != null) {
            uri = data.getData();
        }
        //Ony images captured by camera are scaled for now
        if (mimeType.equals("image/jpeg") && uri == capturedImageUri) {
            return getBytesWithBitmapScaling(uri);
        } else {
            return getBytes(context, uri);
        }
    }

    public static byte[] getBytesWithBitmapScaling(Uri imageUri) {
        if (imageUri == null) {
            return null;
        }
        String filePath = imageUri.getPath();
        BitmapFactory.Options options = new BitmapFactory.Options();

        //  just the bounds of image are loaded, not actual bitmap pixels
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        float actualHeight = options.outHeight;
        float actualWidth = options.outWidth;

        float maxHeight = DEFAULT_IMAGE_MAX_HEIGHT;
        float maxWidth = DEFAULT_IMAGE_MAX_WIDTH;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

        //  width and height values are set maintaining the aspect ratio of the image
        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = imgRatio * actualWidth;
                actualHeight = maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = imgRatio * actualHeight;
                actualWidth = maxWidth;
            } else {
                actualHeight = maxHeight;
                actualWidth = maxWidth;
            }
        }

        //  setting inSampleSize value allows to load a scaled down version of the original image
        options.inSampleSize = calculateInSampleSize(options, (int) actualWidth, (int) actualHeight);

        // set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

        //  this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        Bitmap bitmap;
        Bitmap scaledBitmap;
        try {
            bitmap = BitmapFactory.decodeFile(filePath, options);
            scaledBitmap = Bitmap.createBitmap((int) actualWidth, (int) actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            MobileMessagingLogger.e("[InAppChat] can't load image to send attachment", exception);
            return null;
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        //  check the rotation of the image and display it properly
        int orientationDegree = getExifOrientationDegree(filePath);
        Matrix rotationMatrix = new Matrix();
        rotationMatrix.postRotate(orientationDegree);
        scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotationMatrix, true);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        bitmap.recycle();
        scaledBitmap.recycle();
        return outputStream.toByteArray();
    }

    private static int getExifOrientationDegree(String filepath) {
        int degree = 0;
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filepath);
        } catch (IOException exception) {
            MobileMessagingLogger.e("[InAppChat] can't get image orientation", exception);
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

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = Math.min(heightRatio, widthRatio);
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }

    private static byte[] getBytes(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
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