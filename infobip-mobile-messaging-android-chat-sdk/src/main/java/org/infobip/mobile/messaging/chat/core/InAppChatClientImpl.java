package org.infobip.mobile.messaging.chat.core;

import android.webkit.WebView;

import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.StringUtils;

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
    public void sendChatMessage(String message, InAppChatMobileAttachment attachment) {
        // message can be null - its OK
        String base64UrlString = attachment.base64UrlString();
        String fileName = attachment.getFileName();
        // FIXME [CHAT-821]: for old version of Android attachment`s max size is very small
        if (webView != null && isNotBlank(base64UrlString)) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                String script = buildWidgetMethodInvocation(InAppChatWidgetMethods.handleMessageWithAttachmentSend.name(), false, message, base64UrlString, fileName);
                webView.evaluateJavascript(script, null);
            } else {
                webView.loadUrl(buildWidgetMethodInvocation(InAppChatWidgetMethods.handleMessageWithAttachmentSend.name(), message, base64UrlString, fileName));
            }
        } else {
            MobileMessagingLogger.e("[InAppChat] can't send attachment, base64 is empty");
        }
    }

    private String buildWidgetMethodInvocation(String methodName, String... params) {
        return this.buildWidgetMethodInvocation(methodName, true, params);
    }

    private String buildWidgetMethodInvocation(String methodName, boolean withPrefix, String... params) {
        StringBuilder builder = new StringBuilder();
        if (withPrefix) {
            builder.append("javascript:");
        }
        builder.append(methodName);

        if (params.length > 0) {
            String resultParamsStr = StringUtils.join("','", "('", "')", params);
            builder.append(resultParamsStr);
        }
        return builder.toString();
    }
}