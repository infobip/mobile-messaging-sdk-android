package org.infobip.mobile.messaging.chat.core;

import android.webkit.WebView;

import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachment;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import static org.infobip.mobile.messaging.util.StringUtils.isNotBlank;

public class InAppChatClientImpl implements InAppChatClient {

    private final WebView webView;

    public InAppChatClientImpl(WebView webView) {
        this.webView = webView;
    }

    @Override
    public void sendChatMessage(String message) {
        if (webView != null && isNotBlank(message)) {
            webView.loadUrl(buildWidgetMethodInvocation(InAppChatWidgetMethods.handleMessageSend.name(), message));
        }
    }

    @Override
    public void sendChatMessage(String message, InAppChatAttachment attachment) {
        // message can be null - its OK
        String base64UrlString = attachment.base64UrlString();
        if (webView != null && isNotBlank(base64UrlString)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(buildWidgetMethodInvocation(InAppChatWidgetMethods.handleMessageWithAttachmentSend.name(), message, base64UrlString).replaceFirst("javascript:","")+";", null);
            } else {
                webView.loadUrl(buildWidgetMethodInvocation(InAppChatWidgetMethods.handleMessageWithAttachmentSend.name(), message, base64UrlString));
            }
        } else {
            MobileMessagingLogger.e("[InAppChat] can't send attachment, base64 is empty");
        }
    }

    private String buildWidgetMethodInvocation(String methodName, String... params) {
        StringBuilder builder = new StringBuilder();
        builder
                .append("javascript:")
                .append(methodName)
                .append("(");
        if (params.length > 0) {
            for (String param : params) {
                builder
                        .append("'")
                        .append(param)
                        .append("'")
                        .append(", ");
            }
        }
        builder.delete(builder.lastIndexOf(","), builder.length());
        builder.append(")");
        return builder.toString();
    }
}