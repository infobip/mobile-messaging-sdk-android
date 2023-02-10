package org.infobip.mobile.messaging.chat.core;

import android.app.Activity;
import android.os.Handler;
import android.webkit.JavascriptInterface;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

public class InAppChatMobileImpl implements InAppChatMobile {

    private final InAppChatWebViewManager inAppChatWebViewManager;
    private Handler handler;

    public InAppChatMobileImpl(InAppChatWebViewManager inAppChatWebViewManager) {
        this.inAppChatWebViewManager = inAppChatWebViewManager;
        if (inAppChatWebViewManager instanceof Fragment) {
            FragmentActivity activity = ((Fragment) inAppChatWebViewManager).getActivity();
            if (activity != null) {
                this.handler = new Handler(activity.getMainLooper());
            }
        } else if (inAppChatWebViewManager instanceof Activity) {
            Activity inAppChatWebViewManagerActivity = (Activity) inAppChatWebViewManager;
            this.handler = new Handler(inAppChatWebViewManagerActivity.getMainLooper());
        }
    }

    @Override
    @JavascriptInterface
    public void setControlsEnabled(final boolean isEnabled) {
        MobileMessagingLogger.d("WebView setting controls enabled: " + isEnabled);
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if (inAppChatWebViewManager != null)
                    inAppChatWebViewManager.setControlsEnabled(isEnabled);
            }
        };
        handler.post(myRunnable);
    }

    @Override
    @JavascriptInterface
    public void onError(final String errorMessage) {
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                MobileMessagingLogger.d("WebView loading error", errorMessage);
                if (inAppChatWebViewManager != null) inAppChatWebViewManager.onJSError(errorMessage);
            }
        };
        handler.post(myRunnable);
    }

    @Override
    @JavascriptInterface
    public void setControlsVisibility(final boolean isVisible) {
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                MobileMessagingLogger.d("WebView input visible: " + isVisible);
                if (inAppChatWebViewManager != null) inAppChatWebViewManager.setControlsVisibility(isVisible);
            }
        };
        handler.post(myRunnable);
    }

    @Override
    @JavascriptInterface
    public void openAttachmentPreview(final String url, final String type, final String caption) {
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if (inAppChatWebViewManager != null) {
                    inAppChatWebViewManager.openAttachmentPreview(url, type, caption);
                }
            }
        };
        handler.post(myRunnable);
    }

    @Override
    @JavascriptInterface
    public void onViewChanged(String view) {
        Runnable myRunnable = () -> {
            MobileMessagingLogger.d("WebView onViewChanged: " + view);
            if (inAppChatWebViewManager != null) {
                try {
                    inAppChatWebViewManager.onWidgetViewChanged(InAppChatWidgetView.valueOf(view));
                } catch (IllegalArgumentException exception) {
                    MobileMessagingLogger.e("Could not parse InAppChatWidgetView from " + view, exception);
                }
            }
        };
        handler.post(myRunnable);
    }
}
