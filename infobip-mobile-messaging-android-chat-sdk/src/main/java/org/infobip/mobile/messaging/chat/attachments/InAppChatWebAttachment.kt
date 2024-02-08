package org.infobip.mobile.messaging.chat.attachments

import android.webkit.URLUtil

data class InAppChatWebAttachment(
    val url: String,
    val fileName: String,
) {
    companion object {
        operator fun invoke(
            url: String,
            contentDisposition: String? = null,
            mimeType: String? = null,
        ): InAppChatWebAttachment {
            val fileName = URLUtil.guessFileName(url, parseContentDisposition(contentDisposition).orEmpty(), mimeType)
            return InAppChatWebAttachment(url, fileName)
        }

        /**
         * UrlUtil.guessFileName has a problem parsing contentDisposition format from cloudfront hosting
         * This method returns only fileName part
         */
        private fun parseContentDisposition(contentDisposition: String?): String? {
            if (contentDisposition.isNullOrEmpty()) {
                return null
            }
            return try {
                var fileName = contentDisposition.substringAfter("filename=").substringBefore(";").removeSurrounding("\"")
                val type = fileName.substringAfterLast(".")
                // files from LIVE_CHAT channel contain file type suffix twice...
                val occurrences = fileName.windowed(type.length) { if (it == type) 1 else 0 }.sum()
                if (occurrences > 1) {
                    fileName = fileName.removeSuffix(".$type")
                }
                return "attachment; filename=$fileName"
            } catch (e: Exception) {
                null
            }
        }
    }
}