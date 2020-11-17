package org.infobip.mobile.messaging.view;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.R;
import org.infobip.mobile.messaging.app.WebViewSettingsResolver;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.StringUtils;

import java.net.URISyntaxException;


public class WebViewActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "ib_webview_url";

    private ProgressBar progressBar;
    private WebView webView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        WebViewSettingsResolver webViewSettingsResolver = new WebViewSettingsResolver(this);
        setTheme(webViewSettingsResolver.getWebViewTheme());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ib_activity_webview);
        Toolbar toolbar = findViewById(R.id.ib_toolbar_webview);
        setSupportActionBar(toolbar);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            onBackPressed();
            return;
        }
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);

        final TextView tvToolbarTitle = toolbar.findViewById(R.id.ib_tv_webview_toolbar_title);
        tvToolbarTitle.setText(toolbar.getTitle());
        applyStylesFromConfig(toolbar, tvToolbarTitle);

        Intent webViewIntent = getIntent();
        String url = webViewIntent.getStringExtra(EXTRA_URL);
        webView = findViewById(R.id.ib_webview);

        WebSettings settings = webView.getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String innerUrl) {
                return WebViewActivity.this.shouldOverrideUrlLoading(innerUrl);
            }

            public void onPageFinished(WebView view, String url) {
                try {
                    progressBar.setVisibility(View.GONE);
                    tvToolbarTitle.setText(view.getTitle());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        webView.loadUrl(url);
    }

    /**
     * Starts resolved activity (in another app) if:
     * <ul>
     *     <li>URL starts with "intent://" scheme (In this case if it cannot be opened it falls back to "browser_fallback_url" which is
     *     provided within parsedUri intent or Google Play / App Gallery is opened with the package name to install the app)</li>
     *     <li>parsed URI from http(s) URL or any other scheme can be loaded</li>
     * </ul>
     * e.g. Parsed URI starts with "intent://" when "https://play.google.com/store/apps/details?id=com.infobip.pushdemo" was received
     * as a webviewUrl and default webview action to open it in app (Play Store) was clicked
     */
    private boolean shouldOverrideUrlLoading(String innerUrl) {
        if (StringUtils.isBlank(innerUrl)) return false;

        final PackageManager packageManager = getPackageManager();
        // try to open intent scheme
        if (innerUrl.startsWith("intent://")) {
            try {
                Intent intent = Intent.parseUri(innerUrl, Intent.URI_INTENT_SCHEME);
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent);
                    return true;
                }
                // no resolved activity, try to find fallback URL
                String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                if (fallbackUrl != null) {
                    webView.loadUrl(fallbackUrl);
                    return true;
                }
                // invite to install app on Google Play
                Intent marketIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("market://details?id=" + intent.getPackage()));
                if (marketIntent.resolveActivity(packageManager) != null) {
                    startActivity(marketIntent);
                    return true;
                }
                // invite to install app on App Gallery
                Intent appMarketIntent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse("appmarket://details?id=" + intent.getPackage()));
                if (appMarketIntent.resolveActivity(packageManager) != null) {
                    startActivity(appMarketIntent);
                    return true;
                }
            } catch (URISyntaxException e) {
                // not an intent URI
                MobileMessagingLogger.w("Failed to resolve intent:// for web view URL " + innerUrl, e);
            }
        }

        // try to open any scheme/activity that can be resolved
        Uri parsedUri = Uri.parse(innerUrl);
        if (parsedUri != null) {
            Intent parsedUriIntent = new Intent(Intent.ACTION_VIEW, parsedUri);
            if (parsedUriIntent.resolveActivity(packageManager) != null) {
                startActivity(parsedUriIntent);
                return true;
            }
        }

        return false;
    }

    private void applyStylesFromConfig(Toolbar toolbar, TextView tvToolbarTitle) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        // toolbar background color
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        toolbar.setBackgroundColor(typedValue.data);

        tvToolbarTitle.setTextAppearance(this, R.style.TextAppearance_AppCompat_Widget_ActionBar_Title);

        theme.resolveAttribute(R.attr.titleTextColor, typedValue, true);
        tvToolbarTitle.setTextColor(typedValue.data);

        progressBar = findViewById(R.id.ib_pb_webview);
        theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
        try {
            progressBar.getIndeterminateDrawable().setColorFilter(typedValue.data, PorterDuff.Mode.SRC_IN);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.ib_menu_cancel) {
            goBack();
            webView.freeMemory();
            webView.removeAllViews();
            webView.destroy();
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ib_menu_webview, menu);
        ////tint x button
        Drawable drawable = tintXButton(menu);
        menu.findItem(R.id.ib_menu_cancel).setIcon(drawable);
        return true;
    }

    @NonNull
    private Drawable tintXButton(Menu menu) {
        Drawable drawable = menu.findItem(R.id.ib_menu_cancel).getIcon();
        drawable = DrawableCompat.wrap(drawable);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        theme.resolveAttribute(R.attr.colorControlNormal, typedValue, true);
        drawable.setColorFilter(typedValue.data, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    @Override
    public void onBackPressed() {
        if (!webView.canGoBack()) {
            goBack();
            return;
        }
        webView.goBack();
    }

    private void goBack() {
        if (isTaskRoot()) {
            NotificationSettings notificationSettings = MobileMessagingCore.getInstance(WebViewActivity.this).getNotificationSettings();
            if (notificationSettings != null) {
                Class<?> callbackActivity = notificationSettings.getCallbackActivity();
                Intent startParentActivity = new Intent(WebViewActivity.this, callbackActivity);
                startActivity(startParentActivity);
            }
        }
        super.onBackPressed();
    }
}