package org.infobip.mobile.messaging.chat.models

import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.annotation.StringRes
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.core.InAppChatException

/**
 * Represents the source of the attachment.
 */
enum class AttachmentSource {
    VisualMediaPicker,
    Camera,
    VideoRecorder,
    FilePicker
}

/**
 * Specification of the attachment source.
 * Each source has its own restrictions and requirements. Specification is used to store those requirements per source.
 */
internal sealed class AttachmentSourceSpecification(
    val attachmentSource: AttachmentSource,
    @StringRes val nameRes: Int,
) {

    data class Camera @Throws(InAppChatException::class) constructor(
        val photoFileExtension: String,
    ) : AttachmentSourceSpecification(AttachmentSource.Camera, R.string.ib_chat_attachments_take_a_photo) {
        companion object {
            //Order is important, sorted by best compatibility on Android
            val allowedFileExtension: Set<String> = setOf("jpg", "jpeg", "png", "webp", "heic", "bmp")
        }

        init {
            if (allowedFileExtension.none { photoFileExtension.equals(it, ignoreCase = true) }) {
                throw InAppChatException.InvalidPhotoAttachmentExtension()
            }
        }
    }

    data class VideoRecorder @Throws(InAppChatException::class) constructor(
        val videoFileExtension: String,
    ) : AttachmentSourceSpecification(AttachmentSource.VideoRecorder, R.string.ib_chat_attachments_record_video) {
        companion object {
            //Order is important, sorted by best compatibility on Android
            val allowedFileExtension: Set<String> = setOf("mp4", "3gp", "webm", "m4v", "mpeg", "mpg", "mov", "m1v", "m2v", "mpe", "mp4v", "mpg4")
        }

        init {
            if (allowedFileExtension.none { videoFileExtension.equals(it, ignoreCase = true) }) {
                throw InAppChatException.InvalidVideoAttachmentExtension()
            }
        }
    }

    data class FilePicker(
        val mimeTypes: Set<String>
    ) : AttachmentSourceSpecification(AttachmentSource.FilePicker, R.string.ib_chat_attachments_select_file)

    data class VisualMediaPicker(
        val type: VisualMediaType
    ) : AttachmentSourceSpecification(AttachmentSource.VisualMediaPicker, R.string.ib_chat_attachments_select_from_gallery)

}