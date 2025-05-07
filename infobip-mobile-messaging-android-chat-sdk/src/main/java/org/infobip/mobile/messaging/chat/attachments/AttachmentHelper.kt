/*
 * Mobile Messaging Android Showcase app
 * AttachmentHelper.kt
 *
 * Created by jdzubak on 12/12/2024.
 * Copyright © 2024 Infobip Ltd. All rights reserved.
 */

package org.infobip.mobile.messaging.chat.attachments

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import org.infobip.mobile.messaging.chat.utils.fileName
import org.infobip.mobile.messaging.chat.utils.mimeType
import java.io.ByteArrayOutputStream
import java.util.UUID


internal object AttachmentHelper {

    private const val tag = "AttachmentHelper"

    val ATTACHMENT_URL_REGEX = Regex("""(?<prefix>data:)(?<mimeType>[^;]+)(?<base64Prefix>;base64,)(?<base64Value>[A-Za-z0-9+\\/=\n]+)""")

    @Throws(IllegalStateException::class)
    @JvmStatic
    fun createInAppChatAttachment(
        context: Context,
        uri: Uri,
        maxBytesSize: Int = InAppChatMobileAttachment.DEFAULT_MAX_UPLOAD_CONTENT_SIZE.toInt()
    ): InAppChatMobileAttachment {
        val mimeType: String? = uri.mimeType(context)
        if (mimeType.isNullOrBlank())
            throw IllegalStateException("Failed to get mime type")
        val contentResolver: ContentResolver? = context.contentResolver
        if (contentResolver != null) {
            val inputStream = contentResolver.openInputStream(uri)
            val data: ByteArray?
            if (mimeType == InAppChatAttachmentHelper.MIME_TYPE_IMAGE_JPEG) {
                val bitmap: Bitmap = BitmapFactory.decodeStream(inputStream) ?: throw IllegalStateException("Failed to decode bitmap")
                val scaledBitmap = scaleCompressAndRotateBitmap(context, uri, bitmap, maxBytesSize)
                data = (scaledBitmap ?: bitmap).toByteArray()
                bitmap.recycle()
                scaledBitmap?.recycle()
            } else {
                data = inputStream?.use { it.readBytes() }
            }

            if (data == null)
                throw IllegalStateException("Attachment data is null")
            if (data.isEmpty())
                throw IllegalStateException("Attachment data is empty")
            if (data.size > maxBytesSize)
                throw IllegalStateException("Attachment data is too large")

            val encodedData: String? = Base64.encodeToString(data, Base64.DEFAULT)
            val fileName = requireFileName(context, uri, mimeType)
            return if (encodedData?.isNotBlank() == true && fileName.isNotBlank())
                InAppChatMobileAttachment(mimeType, encodedData, fileName)
            else
                throw IllegalStateException("Attachment encoded data or file name is blank")
        } else {
            throw IllegalStateException("Could not get content resolver")
        }
    }

    //region file name
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
    private fun requireFileName(context: Context, uri: Uri, mimeType: String): String {
        var fileName: String?
        fileName = uri.fileName(context)
        if (fileName?.isNotBlank() == true)
            return fileName
        fileName = uri.lastPathSegment
        if (fileName.isNullOrBlank()) {
            fileName = UUID.randomUUID().toString()
        }
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (extension != null) {
            fileName += ".$extension"
        }
        return fileName
    }
    //endregion

    //region bitmap scaling and compression
    private fun Bitmap.toByteArray(): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun scaleCompressAndRotateBitmap(
        context: Context,
        uri: Uri,
        originalBitmap: Bitmap,
        targetSizeInBytes: Int
    ): Bitmap? {
        return runCatching {
            val cr = context.applicationContext.contentResolver
            if (cr == null) {
                Log.e(tag, "Failed to get content resolver")
                return null
            }
            var scaledBitmap = originalBitmap
            var quality = 100
            val byteArrayOutputStream = ByteArrayOutputStream()
            do {
                byteArrayOutputStream.reset()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)

                if (byteArrayOutputStream.size() <= targetSizeInBytes) {
                    break
                }
                quality -= 10
                if (quality <= 0) {
                    break
                }
            } while (byteArrayOutputStream.size() > targetSizeInBytes)

            if (byteArrayOutputStream.size() > targetSizeInBytes) {
                scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 800, 800, true)
                return scaleCompressAndRotateBitmap(context, uri, scaledBitmap, targetSizeInBytes)
            }

            val resultBitmap: Bitmap = BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size())
            val orientation = getImageOrientation(context, uri)
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(resultBitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(resultBitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(resultBitmap, 270f)
                else -> resultBitmap
            }
        }.onFailure {
            Log.e(tag, "Failed to scale and compress bitmap", it)
        }.getOrNull()
    }

    private fun getImageOrientation(
        context: Context,
        uri: Uri,
    ): Int? {
        val cr = context.applicationContext.contentResolver
        if (cr == null) {
            Log.e(tag, "Failed to get content resolver")
            return null
        }
        val ei = runCatching {
            cr.openInputStream(uri)?.let { ExifInterface(it) }
                ?: uri.path?.let { ExifInterface(it) }
        }.getOrNull()
        return ei?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
    }

    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    //endregion

}