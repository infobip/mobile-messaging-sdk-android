package org.infobip.mobile.messaging.chat.view;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.chat.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.R;
import org.infobip.mobile.messaging.chat.core.InAppChatClient;
import org.infobip.mobile.messaging.chat.core.InAppChatClientImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatMobileImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewClient;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewManager;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

public class InAppChatActivity extends AppCompatActivity implements InAppChatWebViewManager {

    private static final String IN_APP_CHAT_MOBILE_INTERFACE = "InAppChatMobile";
    private static final String RES_ID_IN_APP_CHAT_WIDGET_URI = "ib_inappchat_widget_uri";

    private boolean sendButtonIsColored;
    private WidgetInfo widgetInfo;

    /* View components */
    private WebView webView;
    private EditText editText;
    private ImageView btnSend;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private RelativeLayout relativeLayout;
    private TextView chatNotAvailableView;
    private InAppChatClient inAppChatClient;
    private InAppChatViewSettingsResolver inAppChatViewSettingsResolver;
    private Boolean shouldUseWidgetConfig = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        inAppChatViewSettingsResolver = new InAppChatViewSettingsResolver(this);
        setTheme(inAppChatViewSettingsResolver.getChatViewTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ib_activity_chat);
        String widgetUri = ResourceLoader.loadStringResourceByName(this, RES_ID_IN_APP_CHAT_WIDGET_URI);

        initWidgetInfo();
        if (widgetInfo != null) {
            initViews();
            loadWebPage(widgetUri);
        }
    }

    private void initWidgetInfo() {
        SharedPreferences prefs = PropertyHelper.getDefaultMMSharedPreferences(this);
        String widgetId = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID.getKey(), null);
        String widgetTitle = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE.getKey(), null);
        String widgetPrimaryColor = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.getKey(), null);
        String widgetBackgroundColor = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.getKey(), null);

        if (widgetId != null) {
            widgetInfo = new WidgetInfo(widgetId, widgetTitle, widgetPrimaryColor, widgetBackgroundColor);
        }
    }

    private void initViews() {
        progressBar = findViewById(R.id.ib_pb_chat);
        relativeLayout = findViewById(R.id.ib_rl_send_message);
        chatNotAvailableView = findViewById(R.id.ib_tv_chat_not_available);
        initToolbar();
        initWebView();
        initTextBar();
        initSendButton();
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.ib_toolbar_chat);
        setUpToolbar(toolbar, inAppChatViewSettingsResolver.getChatViewTitle());
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeChatPage();
            }
        });
    }

    public void closeChatPage() {
        webView.freeMemory();
        webView.removeAllViews();
        webView.destroy();
        finish();
    }

    private void initTextBar() {
        editText = findViewById(R.id.ib_et_message_text);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0 && !sendButtonIsColored) {
                    fillSendButtonByPrimaryColor();
                    sendButtonIsColored = true;
                } else if (s.length() == 0) {
                    btnSend.getDrawable().clearColorFilter();
                    sendButtonIsColored = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // nothing
            }
        });
    }

    private void fillSendButtonByPrimaryColor() {
        @ColorInt int widgetPrimaryColor = Color.parseColor(widgetInfo.getPrimaryColor());
        if (!shouldUseWidgetConfig()) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getTheme();
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
            widgetPrimaryColor = typedValue.data;
        }
        btnSend.getDrawable().setColorFilter(widgetPrimaryColor, PorterDuff.Mode.SRC_ATOP);
    }

    protected void setUpToolbar(Toolbar toolbar, String titleFromRes) {
        if (toolbar == null) return;

        setSupportActionBar(toolbar);

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) {
            toolbar.setNavigationIcon(R.drawable.ic_menu_white);
            return;
        }
        supportActionBar.setDisplayHomeAsUpEnabled(true);

        @ColorInt int primaryColor = Color.parseColor(widgetInfo.getPrimaryColor());
        @ColorInt int titleTextColor = Color.parseColor(widgetInfo.getBackgroundColor());
        @ColorInt int navigationIconColor = titleTextColor;
        @ColorInt int primaryDarkColor = calculatePrimaryDarkColor(primaryColor);

        // setup colors (from widget or local config)
        if (shouldUseWidgetConfig()) {
            setStatusBarColor(primaryDarkColor);
        } else {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getTheme();
            // toolbar background color
            theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
            if (typedValue.data != 0) primaryColor = typedValue.data;
            // titleFromRes text color
            theme.resolveAttribute(R.attr.titleTextColor, typedValue, true);
            if (typedValue.data != 0) titleTextColor = typedValue.data;
            // back arrow color
            theme.resolveAttribute(R.attr.colorControlNormal, typedValue, true);
            if (typedValue.data != 0) navigationIconColor = typedValue.data;

            theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
            if (typedValue.data != 0) primaryDarkColor = typedValue.data;
        }

        // set colors to views
        try {
            progressBar.getIndeterminateDrawable().setColorFilter(primaryDarkColor, PorterDuff.Mode.SRC_IN);
        } catch (Exception ignored) {
        }
        toolbar.setBackgroundColor(primaryColor);

        String title = titleFromRes;
        if (StringUtils.isBlank(titleFromRes)) {
            title = widgetInfo.getTitle();
        }
        supportActionBar.setTitle(title);
        toolbar.setTitleTextColor(titleTextColor);

        Drawable drawable = toolbar.getNavigationIcon();
        if (drawable != null) {
            drawable.setColorFilter(navigationIconColor, PorterDuff.Mode.SRC_ATOP);
        }
    }

    private int calculatePrimaryDarkColor(int primaryColor) {
        return ColorUtils.blendARGB(primaryColor, Color.BLACK, 0.2f);
    }

    private void setStatusBarColor(int statusBarColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(statusBarColor);
        }
    }

    private boolean shouldUseWidgetConfig() {
        if (shouldUseWidgetConfig != null) return shouldUseWidgetConfig;

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        int primaryColor = typedValue.data;

        theme.resolveAttribute(R.attr.colorControlNormal, typedValue, true);
        int colorControlNormal = typedValue.data;

        theme.resolveAttribute(R.attr.titleTextColor, typedValue, true);
        int titleTextColor = typedValue.data;

        shouldUseWidgetConfig = (primaryColor == colorControlNormal) && (colorControlNormal == titleTextColor);
        return shouldUseWidgetConfig;
    }

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    private void initWebView() {
        webView = findViewById(R.id.ib_wv_in_app_chat);

        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        webViewSettings.setDomStorageEnabled(true);

        webView.setClickable(true);
        webView.setWebViewClient(new InAppChatWebViewClient(this));
        webView.addJavascriptInterface(new InAppChatMobileImpl(this), IN_APP_CHAT_MOBILE_INTERFACE);

        inAppChatClient = new InAppChatClientImpl(webView);
    }

    private void initSendButton() {
        btnSend = findViewById(R.id.ib_btn_send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Editable text = editText.getText();
                if (text != null) {
                    inAppChatClient.sendChatMessage(escapeString(text.toString()));
                    text.clear();
                }
            }
        });
    }

    private String escapeString(String source) {
        String serialize = new JsonSerializer().serialize(source);
        return serialize.substring(1, serialize.length() - 1);
    }

    private void loadWebPage(String url) {
        String pushRegistrationId = MobileMessagingCore.getInstance(this).getPushRegistrationId();
        if (pushRegistrationId != null && webView != null && widgetInfo != null) {
            String resultUrl = new Uri.Builder()
                    .encodedPath(url)
                    .appendQueryParameter("pushRegId", pushRegistrationId)
                    .appendQueryParameter("widgetId", widgetInfo.getId())
                    .build()
                    .toString();
            webView.loadUrl(resultUrl);
        }
    }

    @Override
    public void onPageStarted() {
        progressBar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPageFinished() {
        progressBar.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    @Override
    public void setControlsEnabled(boolean isEnabled) {
        editText.setEnabled(isEnabled);
        btnSend.setEnabled(isEnabled);
    }

    @Override
    public void setInvisible() {
        chatNotAvailableView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        relativeLayout.setVisibility(View.GONE);
    }
}
