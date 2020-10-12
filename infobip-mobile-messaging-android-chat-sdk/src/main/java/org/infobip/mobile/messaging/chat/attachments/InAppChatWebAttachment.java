package org.infobip.mobile.messaging.chat.attachments;

import android.webkit.URLUtil;

public class InAppChatWebAttachment {
    String url;
    String contentDisposition;
    String mimeType;
    String fileName;

    public InAppChatWebAttachment(String url, String contentDisposition, String mimeType) {
        this.url = url;
        this.contentDisposition = contentDisposition;
        this.mimeType = mimeType;
        this.fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }
}
