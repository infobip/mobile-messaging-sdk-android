package org.infobip.mobile.messaging.view;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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
                return false;
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