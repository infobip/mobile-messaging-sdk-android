package org.infobip.mobile.messaging.chat.view;

import static org.infobip.mobile.messaging.chat.utils.CommonUtils.isOSOlderThanKitkat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.chat.InAppChatImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatMobileImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewClient;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewManager;
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.chat.utils.DarkModeUtils;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatDarkMode;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

public class InAppChatWebView extends WebView {
    private static final String IN_APP_CHAT_MOBILE_INTERFACE = "InAppChatMobile";
    private static final String RES_ID_IN_APP_CHAT_WIDGET_URI = "ib_inappchat_widget_uri";

    private String widgetUri;

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
        setForceDarkAllowed();
        setClickable(true);
        setWebViewClient(new InAppChatWebViewClient(webViewManager));
        addJavascriptInterface(new InAppChatMobileImpl(webViewManager), IN_APP_CHAT_MOBILE_INTERFACE);
    }

    private void setForceDarkAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String darkModeName = new PropertyHelper(getContext()).findString(MobileMessagingChatProperty.IN_APP_CHAT_DARK_MODE);
            setForceDarkAllowed(InAppChatDarkMode.DARK_MODE_YES.name().equals(darkModeName) || InAppChatDarkMode.DARK_MODE_FOLLOW_SYSTEM.name().equals(darkModeName));
            MobileMessagingLogger.d("ForceDark allowed in WebView: " + isForceDarkAllowed());
        }
    }

    public void loadWebPage(Boolean force, WidgetInfo widgetInfo, String jwt) {
        if (!force && !InAppChatImpl.getIsWebViewCacheCleaned()) {
            return;
        }
        InAppChatImpl.setIsWebViewCacheCleaned(false);

        String pushRegistrationId = MobileMessagingCore.getInstance(getContext()).getPushRegistrationId();
        if (pushRegistrationId != null && widgetInfo != null) {
            Uri.Builder builder = new Uri.Builder()
                    .encodedPath(widgetUri)
                    .appendQueryParameter("pushRegId", pushRegistrationId)
                    .appendQueryParameter("widgetId", widgetInfo.getId());

            if (StringUtils.isNotBlank(jwt)) {
                builder.appendQueryParameter("jwt", jwt);
            }

            String resultUrl = builder.build().toString();
            loadUrl(resultUrl);
        }
    }

    public void evaluateJavascriptMethod(String script, ValueCallback<String> resultCallback) {
        if (isOSOlderThanKitkat()) {
            // FIXME: not safety call. Can be reason of invisible OutOfMemory error (data transfer limit). More info: [CHAT-821]
            this.loadUrl(script);
        } else {
            this.evaluateJavascript(script, resultCallback);
        }
    }
}
