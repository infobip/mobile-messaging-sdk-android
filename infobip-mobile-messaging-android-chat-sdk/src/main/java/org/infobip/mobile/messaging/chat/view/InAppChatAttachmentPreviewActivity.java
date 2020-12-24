package org.infobip.mobile.messaging.chat.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.infobip.mobile.messaging.ConfigurationException;
import org.infobip.mobile.messaging.chat.R;
import org.infobip.mobile.messaging.chat.attachments.InAppChatWebAttachment;
import org.infobip.mobile.messaging.chat.attachments.PermissionsRequestManager;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.ResourceLoader;


public class InAppChatAttachmentPreviewActivity extends AppCompatActivity implements PermissionsRequestManager.PermissionsRequester {

    public static final String EXTRA_URL = "ib_chat_attachment_url";
    public static final String EXTRA_TYPE = "ib_chat_attachment_type";
    public static final String EXTRA_CAPTION = "ib_chat_attachment_caption";

    private static final String RES_ID_IN_APP_CHAT_ATTACH_PREVIEW_URI = "ib_inappchat_attachment_preview_uri";

    private WebView webView;
    private ProgressBar progressBar;
    private TextView toolbarTitle;
    private Toolbar toolbar;
    private Intent webViewIntent;
    private InAppChatWebAttachment attachment;
    private PermissionsRequestManager permissionsRequestManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        permissionsRequestManager = new PermissionsRequestManager(this, this);
        InAppChatViewSettingsResolver settingsResolver = new InAppChatViewSettingsResolver(this);
        setTheme(settingsResolver.getChatAttachPreviewTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ib_activity_chat_attach_preview);
        webViewIntent = getIntent();
        initViews();
        loadPreviewPage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onFileDownloadingComplete);
    }

    private void initViews() {
        initToolbar();
        applyStylesFromConfig(toolbar, toolbarTitle);
        initActionBar();
        initWebView();
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.ib_chat_attach_tb);
        setSupportActionBar(toolbar);
        toolbarTitle = toolbar.findViewById(R.id.ib_chat_attach_tb_title);
        toolbarTitle.setText(toolbar.getTitle());
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

        progressBar = findViewById(R.id.ib_chat_attach_pb);
        theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
        try {
            @ColorInt int white = Color.parseColor("#FFFFFF");
            progressBar.getIndeterminateDrawable().setColorFilter(white, PorterDuff.Mode.SRC_IN);
        } catch (Exception ignored) {
        }
    }

    private void initActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            onBackPressed();
            return;
        }
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(false);
    }

    /* PermissionsRequester */

    @NonNull
    @Override
    public String[] requiredPermissions() {
        return new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    @Override
    public void onPermissionGranted() {
        downloadFile();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsRequestManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /* PermissionsRequester endregion */

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    private void initWebView() {
        webView = findViewById(R.id.ib_chat_attach_wv);
        final String title = webViewIntent.getStringExtra(EXTRA_CAPTION);

        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);

        webView.setClickable(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String innerUrl) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                toolbarTitle.setText(title);
            }
        });
        webView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                progressBar.setVisibility(View.VISIBLE);
                attachment = new InAppChatWebAttachment(url, contentDisposition, mimetype);
                downloadFile();
            }
        });

        registerReceiver(onFileDownloadingComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    BroadcastReceiver onFileDownloadingComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            progressBar.setVisibility(View.GONE);
        }
    };

    private void downloadFile() {
        if (!permissionsRequestManager.isRequiredPermissionsGranted()) {
            MobileMessagingLogger.e("[InAppChat] Permissions required for attachments not granted", new ConfigurationException(ConfigurationException.Reason.MISSING_REQUIRED_PERMISSION, Manifest.permission.WRITE_EXTERNAL_STORAGE).getMessage());
            return;
        }
        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(attachment.getUrl()));
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, attachment.getFileName());
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        dm.enqueue(request);
    }

    private void loadPreviewPage() {
        String previewPageUrl = ResourceLoader.loadStringResourceByName(this, RES_ID_IN_APP_CHAT_ATTACH_PREVIEW_URI);
        String attachmentUrl = webViewIntent.getStringExtra(EXTRA_URL);
        String attachmentType = webViewIntent.getStringExtra(EXTRA_TYPE);
        String attachmentCaption = webViewIntent.getStringExtra(EXTRA_CAPTION);

        if (webView != null) {
            String resultUrl = new Uri.Builder()
                    .encodedPath(previewPageUrl)
                    .appendQueryParameter("attachmentUrl", attachmentUrl)
                    .appendQueryParameter("attachmentType", attachmentType)
                    .appendQueryParameter("attachmentCaption", attachmentCaption)
                    .build()
                    .toString();
            webView.loadUrl(resultUrl);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ib_menu_webview, menu);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == org.infobip.mobile.messaging.R.id.ib_menu_cancel) {
            onBackPressed();
            webView.freeMemory();
            webView.removeAllViews();
            webView.destroy();
        }
        return true;
    }
}