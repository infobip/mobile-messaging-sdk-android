package org.infobip.mobile.messaging.chat.core;

import android.webkit.WebView;

public class InAppChatClientImpl implements InAppChatClient {

    private final WebView webView;

    public InAppChatClientImpl(WebView webView) {
        this.webView = webView;
    }

    @Override
    public void sendChatMessage(String message) {
        if (webView != null && message != null && !message.isEmpty()) {
            webView.loadUrl(buildWidgetMethodInvocation(InAppChatWidgetMethods.handleMessageSend.name(), message));
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