package org.infobip.mobile.messaging.chat.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.chat.InAppChat;
import org.infobip.mobile.messaging.chat.InAppChatImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatMobileImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewClient;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewManager;
import org.infobip.mobile.messaging.util.ResourceLoader;

public class InAppChatWebView extends WebView {
    private static final String IN_APP_CHAT_MOBILE_INTERFACE = "InAppChatMobile";
    private static final String RES_ID_IN_APP_CHAT_WIDGET_URI = "ib_inappchat_widget_uri";

    private String widgetUri;
    private boolean alreadyLoaded;

    public InAppChatWebView(Context context) {
        super(context);
    }

    public InAppChatWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InAppChatWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    public void setup(InAppChatWebViewManager webViewManager) {
        widgetUri = ResourceLoader.loadStringResourceByName(getContext(), RES_ID_IN_APP_CHAT_WIDGET_URI);
        WebSettings webViewSettings = getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        webViewSettings.setDomStorageEnabled(true);
        webViewSettings.setDatabaseEnabled(true);

        setClickable(true);
        setWebViewClient(new InAppChatWebViewClient(webViewManager));
        addJavascriptInterface(new InAppChatMobileImpl(webViewManager), IN_APP_CHAT_MOBILE_INTERFACE);
    }

    public void loadWebPage(Boolean force, WidgetInfo widgetInfo) {
        if (!force && !InAppChatImpl.getIsWebViewCacheCleaned()) {
            return;
        }
        InAppChatImpl.setIsWebViewCacheCleaned(false);

        String pushRegistrationId = MobileMessagingCore.getInstance(getContext()).getPushRegistrationId();
        if (pushRegistrationId != null && widgetInfo != null) {
            String resultUrl = new Uri.Builder()
                    .encodedPath(widgetUri)
                    .appendQueryParameter("pushRegId", pushRegistrationId)
                    .appendQueryParameter("widgetId", widgetInfo.getId())
                    .build()
                    .toString();
            loadUrl(resultUrl);
        }
    }

}
