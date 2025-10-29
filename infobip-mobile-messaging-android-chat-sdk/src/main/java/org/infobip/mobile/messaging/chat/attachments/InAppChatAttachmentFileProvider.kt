/*
 * InAppChatAttachmentFileProvider.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.attachments

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import java.io.File

/**
 * [FileProvider] responsible for creating files needed when creating attachment using device camera.
 * All files created by this provider are stored in app internal storage `/DCIM/Infobip` directory and
 * must match path defined in `file_paths.xml`.
 */
@SuppressLint("LongLogTag")
class InAppChatAttachmentFileProvider : FileProvider(R.xml.file_paths) {

    companion object {

        private const val TAG = "InAppChatAttachmentFileProvider"

        /**
         * Creates an empty file for a photo or video.
         * Returns null if failed to create a file.
         */
        fun createFile(
            context: Context,
            fileName: String,
        ): File? {
            return runCatching {
                val appContext = context.applicationContext
                //folder must match the one in the file_paths.xml
                val photosFolder = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DCIM), "Infobip")
                return if (!photosFolder.exists() && !photosFolder.mkdirs())
                    null
                else
                    File(photosFolder, fileName)
            }.onFailure {
                MobileMessagingLogger.e(TAG, "Failed to create file.", it)
            }.getOrNull()
        }

        /**
         * Returns file's [Uri].
         * File must be created by [InAppChatAttachmentFileProvider.createFile], otherwise returns null.
         */
        fun getFileUri(context: Context, file: File?): Uri? {
            return file?.let {
                runCatching {
                    val authority = context.packageName + ".fileprovider"
                    getUriForFile(context.applicationContext, authority, file)
                }.onFailure {
                    MobileMessagingLogger.e(TAG, "Failed to create file Uri.", it)
                }.getOrNull()
            }
        }
    }
}