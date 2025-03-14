package org.infobip.mobile.messaging.chat.attachments

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import org.infobip.mobile.messaging.chat.R
import java.io.File

/**
 * [FileProvider] responsible for creating files needed when creating attachment using device camera.
 * All files created by this provider are stored in app internal storage `/DCIM/Infobip` directory and
 * must match path defined in `file_paths.xml`.
 */
@SuppressLint("LongLogTag")
class InAppChatAttachmentFileProvider : FileProvider(R.xml.file_paths) {

    companion object {

        private const val tag = "InAppChatAttachmentFileProvider"

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
                Log.e(tag, "Failed to create file.", it)
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
                    Log.e(tag, "Failed to create file Uri.", it)
                }.getOrNull()
            }
        }
    }
}