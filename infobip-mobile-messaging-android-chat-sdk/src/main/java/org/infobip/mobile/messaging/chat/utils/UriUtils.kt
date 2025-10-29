/*
 * UriUtils.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachment
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import java.io.File

private const val TAG = "FileUtils"

/**
 * Copies file to public destination.
 *
 * @param directory Destination directory, default value is DCIM/Infobip.
 */
fun Uri.copyFileToPublicDir(context: Context, directory: String = "DCIM/Infobip") {
    runCatching {
        val sourceUri = this
        val fileName = sourceUri.fileName(context) ?: return
        val mimeType = sourceUri.mimeType(context) ?: return

        val collectionUri = when {
            mimeType.startsWith(InAppChatAttachment.IMAGE_MIME_TYPE_PREFIX) -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            mimeType.startsWith(InAppChatAttachment.VIDEO_MIME_TYPE_PREFIX) -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> {
                MobileMessagingLogger.e(TAG, "Failed to copy file to public folder. Unsupported MIME type: $mimeType")
                return
            }
        }

        val contentValues = ContentValues().apply {
            put(MediaColumns.DISPLAY_NAME, fileName)
            put(MediaColumns.MIME_TYPE, mimeType)
            put(MediaColumns.RELATIVE_PATH, directory)
            put(MediaColumns.IS_PENDING, 1) // For Android 10+ (scoped storage)
        }

        val resolver = context.contentResolver
        val uri: Uri? = resolver.insert(collectionUri, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                resolver.openInputStream(sourceUri)?.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            contentValues.clear()
            contentValues.put(MediaColumns.IS_PENDING, 0) // File ready
            resolver.update(it, contentValues, null, null)
        }
    }.onFailure {
        MobileMessagingLogger.e(TAG, "Failed to copy file to public folder.", it)
    }.onSuccess {

    }
}

/**
 * Returns the mime type of the file URI.
 */
fun Uri.mimeType(context: Context): String? {
    return runCatching {
        //get mimeType from content:// - file created by InAppChatAttachmentFileProvider
        var mimeType = context.contentResolver.getType(this)
        //get mimeType from file:// - direct file path
        if (mimeType.isNullOrBlank()) {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(this.toString())
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
        }
        mimeType
    }.onFailure {
        MobileMessagingLogger.e(TAG, "Failed to get file mimeType from URI.", it)
    }.getOrNull()
}

/**
 * Returns the file name of the file URI.
 */
fun Uri.fileName(context: Context): String? {
    return runCatching {
        //get fileName from content:// - file created by InAppChatAttachmentFileProvider
        var fileName: String? = null
        val cursor = context.contentResolver.query(this, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = it.getString(nameIndex)
                }
            }
        }
        //get fileName from file:// - direct file path
        if (fileName.isNullOrBlank()) {
            fileName = this.path?.let { File(it).name }
        }
        fileName
    }.onFailure {
        MobileMessagingLogger.e(TAG, "Failed to get file name from URI.", it)
    }.getOrNull()
}

/**
 * Deletes file defined by URI.
 */
fun Uri.deleteFile(context: Context): Boolean {
    return runCatching {
        //deletes files from content:// - file created by InAppChatAttachmentFileProvider
        val rowsDeleted = context.contentResolver.delete(this, null, null)
        if (rowsDeleted > 0) {
            return true
        }
        //deletes files from file:// - direct file path
        val file = File(this.path ?: return false)
        return if (file.exists()) file.delete() else false
    }.onFailure {
        MobileMessagingLogger.e(TAG, "Failed to delete file.", it)
    }.getOrDefault(false)
}