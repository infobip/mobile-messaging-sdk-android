package org.infobip.mobile.messaging.chat.core;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.StringUtils;

public class InAppChatMobileImpl implements InAppChatMobile {

    private final InAppChatWebViewManager inAppChatWebViewManager;
    private Handler handler;
    private static final String TAG = InAppChatMobile.class.getSimpleName();

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
        } else {
            this.handler = new Handler(Looper.getMainLooper());
        }
    }

    @Override
    @JavascriptInterface
    public void setControlsVisibility(final boolean isVisible) {
        Runnable myRunnable = () -> {
            if (inAppChatWebViewManager != null) inAppChatWebViewManager.setControlsVisibility(isVisible);
        };
        handler.post(myRunnable);
    }

    @Override
    @JavascriptInterface
    public void openAttachmentPreview(final String url, final String type, final String caption) {
        Runnable myRunnable = () -> {
            if (inAppChatWebViewManager != null) inAppChatWebViewManager.openAttachmentPreview(url, type, caption);
        };
        handler.post(myRunnable);
    }

    @Override
    @JavascriptInterface
    public void onViewChanged(String view) {
        Runnable myRunnable = () -> {
            MobileMessagingLogger.d(TAG,"Widget onViewChanged: " + view);
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

    @Override
    @JavascriptInterface
    public void onWidgetApiError(String method, String errorPayload) {
        Runnable myRunnable = () -> {
            String result = StringUtils.isNotBlank(errorPayload) ? " => " + errorPayload : "";
            MobileMessagingLogger.e(TAG,"Widget API call error: " + method + "()" + result);
            if (inAppChatWebViewManager != null) {
                try {
                    inAppChatWebViewManager.onWidgetApiError(InAppChatWidgetApiMethod.valueOf(method), errorPayload);
                } catch (IllegalArgumentException exception) {
                    MobileMessagingLogger.e(TAG,"Could not parse InAppChatWidgetApiMethod from " + method, exception);
                }
            }
        };
        handler.post(myRunnable);
    }

    @Override
    @JavascriptInterface
    public void onWidgetApiSuccess(String method, String successPayload) {
        Runnable myRunnable = () -> {
            String result = StringUtils.isNotBlank(successPayload) ? " => " + successPayload : "";
            MobileMessagingLogger.d(TAG,"Widget API call result: " + method + "()" + result);
            if (inAppChatWebViewManager != null) {
                try {
                    inAppChatWebViewManager.onWidgetApiSuccess(InAppChatWidgetApiMethod.valueOf(method), successPayload);
                } catch (IllegalArgumentException exception) {
                    MobileMessagingLogger.e(TAG,"Could not parse InAppChatWidgetApiMethod from " + method, exception);
                }
            }
        };
        handler.post(myRunnable);
    }
}
