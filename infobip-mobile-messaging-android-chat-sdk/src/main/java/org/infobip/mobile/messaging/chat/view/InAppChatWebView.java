package org.infobip.mobile.messaging.chat.view;


import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.infobip.mobile.messaging.chat.core.InAppChatMobileImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatWebChromeClient;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewClient;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewManager;
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
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
        setWebChromeClient(new InAppChatWebChromeClient());
    }

    private void setForceDarkAllowed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String darkModeName = new PropertyHelper(getContext()).findString(MobileMessagingChatProperty.IN_APP_CHAT_DARK_MODE);
            setForceDarkAllowed(InAppChatDarkMode.DARK_MODE_YES.name().equals(darkModeName) || InAppChatDarkMode.DARK_MODE_FOLLOW_SYSTEM.name().equals(darkModeName));
            MobileMessagingLogger.d("ForceDark allowed in WebView: " + isForceDarkAllowed());
        }
    }

    public void loadChatPage(
            @NonNull String pushRegistrationId,
            @NonNull String widgetId,
            @Nullable String jwt,
            @Nullable String domain
    ) {
        Uri.Builder builder = new Uri.Builder()
                .encodedPath(widgetUri)
                .appendQueryParameter("pushRegId", pushRegistrationId)
                .appendQueryParameter("widgetId", widgetId);

        if (StringUtils.isNotBlank(jwt)) {
            builder.appendQueryParameter("jwt", jwt);
        }

        if (StringUtils.isNotBlank(domain)) {
            builder.appendQueryParameter("domain", domain);
        }

        String resultUrl = builder.build().toString();
        loadUrl(resultUrl);
    }

}
