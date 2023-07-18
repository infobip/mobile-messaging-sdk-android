package org.infobip.mobile.messaging.chat.core;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class InAppChatWebViewClient extends WebViewClient {

    private final InAppChatWebViewManager inAppChatWebViewManager;

    public InAppChatWebViewClient(InAppChatWebViewManager inAppChatWebViewManager) {
        this.inAppChatWebViewManager = inAppChatWebViewManager;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        if (inAppChatWebViewManager != null) inAppChatWebViewManager.onPageStarted(url);
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
    }

    @Override
    public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
        super.onReceivedHttpError(view, request, errorResponse);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        if (inAppChatWebViewManager != null) inAppChatWebViewManager.onPageFinished(url);
        super.onPageFinished(view, url);
    }

    @Override
    public void onPageCommitVisible(WebView view, String url) {
        super.onPageCommitVisible(view, url);
        view.pageDown(true);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            return true;
        } else {
            return false;
        }
    }
}