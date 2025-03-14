/*
 * Mobile Messaging Android Showcase app
 * FileUtils.kt
 *
 * Created by jdzubak on 6/2/2025.
 * Copyright © 2025 Infobip Ltd. All rights reserved.
 */

package org.infobip.mobile.messaging.chat.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File

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

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, directory)
            put(MediaStore.Images.Media.IS_PENDING, 1) // Mark file as pending (Android 10+)
        }

        val resolver = context.contentResolver
        val uri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                resolver.openInputStream(sourceUri)?.use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0) // File ready
            resolver.update(it, contentValues, null, null)
        }
    }.onFailure {
        Log.e("FileUtils", "Failed to copy file to public folder.", it)
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
        Log.e("FileUtils", "Failed to get file mimeType from URI.", it)
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
        Log.e("FileUtils", "Failed to get file name from URI.", it)
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
        Log.e("FileUtils", "Failed to delete file.", it)
    }.getOrDefault(false)
}