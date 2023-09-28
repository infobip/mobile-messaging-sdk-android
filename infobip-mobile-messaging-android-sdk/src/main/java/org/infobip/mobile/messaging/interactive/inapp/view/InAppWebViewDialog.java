package org.infobip.mobile.messaging.interactive.inapp.view;

import static android.content.Context.WINDOW_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.R;
import org.infobip.mobile.messaging.api.support.CustomApiHeaders;
import org.infobip.mobile.messaging.api.support.util.UserAgentUtil;
import org.infobip.mobile.messaging.app.ActivityLifecycleListener;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.inapp.InAppWebViewMessage;
import org.infobip.mobile.messaging.interactive.inapp.InAppWebViewMessage.InAppWebViewPosition;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.infobip.mobile.messaging.util.UserAgentAdditions;

import java.util.HashMap;
import java.util.Map;

public class InAppWebViewDialog implements InAppWebView, ActivityLifecycleListener {
    private static final String TAG = "[InAppWebViewDialog]";
    private static final int DIALOG_TIMEOUT_MS = 6000; //6 seconds
    private static final int CORNER_RADIUS = 25;
    public static final int WIDTH_PADDING = 16;
    public static final int VERTICAL_SCROLLBAR_SIZE = 30;
    public static final double SCREEN_COVER_PERCENTAGE = 0.85;
    public static final int SCREEN_MARGINS = 16;
    private final Callback callback;
    private AndroidBroadcaster coreBroadcaster;
    private NotificationAction[] action;
    private final DisplayMetrics displayMetrics;
    private PopupWindow popupWindow;
    private WebView webView;
    private final ActivityWrapper activityWrapper;
    private InAppWebViewMessage message;
    private static int PAGE_LOAD_PROGRESS = 0;
    ConnectionTimeoutHandler timeoutHandler = null;
    View dialogView;
    CardView cardView;

    InAppWebViewDialog(Callback callback, ActivityWrapper activityWrapper) {
        this.callback = callback;
        this.activityWrapper = activityWrapper;
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        coreBroadcaster();
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (popupWindow != null) {
            popupWindow.dismiss();
            setActivityLifecycleListener(null);
        }
    }

    /*
     * This Interface is binding JavaScript code from WebView to Android code
     * */
    public class InAppWebViewInterface {

        /**
         * Instantiate the interface and set the context
         */
        InAppWebViewInterface() {
        }

        /**
         * Handling WebView height change
         *
         * @param pageHeight - page height in density points
         */
        @JavascriptInterface
        public void heightRequested(float pageHeight) {
            MobileMessagingLogger.d(TAG, "onHeightChanged: " + pageHeight);
            updatePageHeight(pageHeight);
        }

        /**
         * Dismiss WebView on Close button click
         */
        @JavascriptInterface
        public void close(String closing) {
            MobileMessagingLogger.d(TAG, "close()");
            new Handler(Looper.getMainLooper()).post(() -> {
                doCallbacks(0);
                popupWindow.dismiss();
                callback.dismissed(InAppWebViewDialog.this);
                setActivityLifecycleListener(null);
                webView.destroy();
            });
        }

        /**
         * Handling opening deep links
         *
         * @param deepLink - application screen to be opened
         */
        @JavascriptInterface
        public void openAppPage(String deepLink) {
            MobileMessagingLogger.d(TAG, "openAppPage: " + deepLink);
            message.setDeeplink(deepLink);
            doCallbacks(1);
            clearWebView();
        }

        /**
         * Handling browser opening
         *
         * @param url - url address to be opened in browser
         */
        @JavascriptInterface
        public void openBrowser(String url) {
            MobileMessagingLogger.d(TAG, "openBrowser: " + url);
            message.setBrowserUrl(url);
            doCallbacks(1);
            clearWebView();
        }

        /**
         * Handling WebView height change
         *
         * @param url - url address to be opened in webview
         */
        @JavascriptInterface
        public void openWebView(String url) {
            MobileMessagingLogger.d(TAG, "openWebView: " + url);
            message.setWebViewUrl(url);
            doCallbacks(1);
            clearWebView();
        }

        private void doCallbacks(int actionType) {
            if (isBanner()) {
                coreBroadcaster.notificationTapped(message);
                callback.notificationPressedFor(InAppWebViewDialog.this, message, action[actionType], getActivity());
            } else
                callback.actionButtonPressedFor(InAppWebViewDialog.this, message, null, action[actionType]);
        }
    }

    @Override
    public void show(@NonNull final InAppWebViewMessage message, @NonNull NotificationAction... actions) {
        this.message = message;
        this.action = actions;

        setActivityLifecycleListener(this);

        if (!PreferenceHelper.findBoolean(getActivity(), MobileMessagingProperty.FULL_FEATURE_IN_APPS_ENABLED)) {
            MobileMessagingLogger.e(TAG, "InApp WebView dialog cannot be displayed because Full-featured In-Apps not enabled!");
        } else {
            try {
                getActivity().runOnUiThread(() -> {
                    LayoutInflater inflater = getActivity().getLayoutInflater();
                    dialogView = inflater.inflate(R.layout.ib_inapp_webview, null, false);

                    cardView = dialogView.findViewById(R.id.webview_dialog_card);

                    if (message.type == InAppWebViewMessage.InAppWebViewType.FULLSCREEN) {
                        setCardViewParams(0, false, 0);
                        cardView.getLayoutParams().height = WindowManager.LayoutParams.MATCH_PARENT;
                        popupWindow = new PopupWindow(dialogView, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, false);
                    } else {
                        setCardViewParams(CORNER_RADIUS, true, 2);
                        cardView.getLayoutParams().height = WindowManager.LayoutParams.WRAP_CONTENT;
                        popupWindow = new PopupWindow(dialogView, displayMetrics.widthPixels - WIDTH_PADDING, WindowManager.LayoutParams.WRAP_CONTENT, false);
                    }

                    int screenRotation = ((WindowManager) getActivity().getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                    if (screenRotation == Surface.ROTATION_90 || screenRotation == Surface.ROTATION_270) { //Landscape
                        cardView.getLayoutParams().width = displayMetrics.heightPixels - WIDTH_PADDING;
                    }

                    popupWindow.setContentView(dialogView);

                    if (message.type == InAppWebViewMessage.InAppWebViewType.BANNER) {
                        setDialogAnimations(message.position);
                        popupWindow.setOutsideTouchable(false);
                    } else {
                        popupWindow.setAnimationStyle(android.R.style.Animation);
                        popupWindow.setOutsideTouchable(true);
                    }

                    setupWebViewForDisplaying(cardView);

                    Map<String, String> headers = getAuthorizationHeader();

                    if (headers.isEmpty()) {
                        webView.loadUrl(message.url);
                    } else {
                        webView.loadUrl(message.url, headers);
                    }
                });
            } catch (Exception e) {
                MobileMessagingLogger.e(TAG, "Failed to display webview for message with ID " + message.getMessageId() + " due to: " + e.getMessage());
            }
        }
    }

    private void setCardViewParams(float radius, boolean useCompatPadding, float elevation) {
        cardView.setRadius(radius);
        cardView.setUseCompatPadding(useCompatPadding);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cardView.setElevation(elevation);
        }
    }

    @NonNull
    private Map<String, String> getAuthorizationHeader() {
        String applicationCode = MobileMessagingCore.getApplicationCode(activityWrapper.getActivity());
        UserAgentUtil userAgentUtil = new UserAgentUtil();
        Map<String, String> headers = new HashMap<>();
        if (applicationCode != null) {
            headers.put("Authorization", "App " + applicationCode);
            headers.put("User-Agent", userAgentUtil.getUserAgent(UserAgentAdditions.getLibVersion(), UserAgentAdditions.getAdditions(activityWrapper.getActivity())));
            headers.put("pushregistrationid", CustomApiHeaders.PUSH_REGISTRATION_ID.getValue());
        }
        return headers;
    }

    private Activity getActivity() {
        return activityWrapper.getActivity();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebViewForDisplaying(CardView dialogView) {
        webView = dialogView.findViewById(R.id.ib_webview);

        if (message.type == InAppWebViewMessage.InAppWebViewType.FULLSCREEN)
            webView.getLayoutParams().height = WindowManager.LayoutParams.MATCH_PARENT;
        else
            webView.getLayoutParams().height = WindowManager.LayoutParams.WRAP_CONTENT;

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                timeoutHandler = new ConnectionTimeoutHandler();
                timeoutHandler.execute();
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (timeoutHandler != null) {
                    timeoutHandler.cancel(true);
                    timeoutHandler = null;
                }
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                MobileMessagingLogger.d(TAG, "onPageCommitVisible()");
                if (message.type == InAppWebViewMessage.InAppWebViewType.BANNER)
                    webView.loadUrl("javascript:window.InfobipMobileMessaging.readBodyHeight()");
                if (isBanner())
                    setDialogTimeout(DIALOG_TIMEOUT_MS);
                showWebViewDialog(dialogView, message.position, message.type);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                logWebViewError(errorCode);
                clearWebView();
            }
        });
        webView.setWebChromeClient(new InAppWebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.addJavascriptInterface(new InAppWebViewInterface(), "InAppWebViewInterface");
    }

    private void clearWebView() {
        try {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                if (webView != null) {
                    callback.dismissed(this);
                    webView.clearHistory();
                    webView.clearCache(true);
                    webView.clearFormData();
                    webView.clearMatches();
                    webView.destroy();
                    webView.setVisibility(View.GONE);
                    if (popupWindow.isShowing())
                        popupWindow.dismiss();
                }
                MobileMessagingLogger.d(TAG, "Deleted local history");
            });
        } catch (Exception e) {
            MobileMessagingLogger.e(TAG, "Failed to delete local history due to " + e.getMessage());
        }
    }

    // Dismiss after some seconds
    public void setDialogTimeout(int dialogTimeout) {
        final Handler handler = new Handler();
        final Runnable runnable = () -> {
            if (popupWindow.isShowing()) {
                popupWindow.dismiss();
                callback.dismissed(this);
            }
        };
        popupWindow.setOnDismissListener(() -> handler.removeCallbacks(runnable));
        handler.postDelayed(runnable, dialogTimeout);
    }

    private void setDialogAnimations(InAppWebViewPosition position) {
        MobileMessagingLogger.d(TAG, "setDialogAnimations()");
        if (position == InAppWebViewPosition.TOP) {
            popupWindow.setAnimationStyle(R.style.BannerAnimationTop);
        } else if (position == InAppWebViewPosition.BOTTOM) {
            popupWindow.setAnimationStyle(R.style.BannerAnimationBottom);
        }
    }

    void updatePageHeight(float pageHeight) {
        Rect displayRectangle = new Rect();
        Window window = getActivity().getWindow();

        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        int calculatedPageHeight = convertDpToPixel(pageHeight, getActivity());

        if (message.type == InAppWebViewMessage.InAppWebViewType.FULLSCREEN && calculatedPageHeight > displayMetrics.heightPixels)
            setVerticalScrollBar(VERTICAL_SCROLLBAR_SIZE);

        try {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                int minHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.dialog_banner_min_height);
                int maxHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.dialog_banner_max_height);
                ViewGroup.LayoutParams viewFlowLayout = webView.getLayoutParams();
                if (message.type == InAppWebViewMessage.InAppWebViewType.BANNER) {
                    viewFlowLayout.height = Math.min(maxHeight, Math.max(minHeight, calculatedPageHeight));
                }
                if (message.type == InAppWebViewMessage.InAppWebViewType.POPUP) {
                    maxHeight = (int) ((displayMetrics.heightPixels * SCREEN_COVER_PERCENTAGE) - SCREEN_MARGINS);
                    if (Math.min(maxHeight, Math.max(minHeight, calculatedPageHeight)) == maxHeight) {
                        viewFlowLayout.height = maxHeight;
                    } else if (Math.min(maxHeight, Math.max(minHeight, calculatedPageHeight)) == minHeight) {
                        viewFlowLayout.height = minHeight;
                    }
                }
                webView.setLayoutParams(viewFlowLayout);
            });
        } catch (Exception e) {
            MobileMessagingLogger.e(TAG, "Failed due to " + e.getMessage());
        }
    }

    private void showWebViewDialog(View dialogView, InAppWebViewPosition position, InAppWebViewMessage.InAppWebViewType type) {
        switch (type) {
            case BANNER:
                if (position == InAppWebViewPosition.TOP)
                    popupWindow.showAtLocation(dialogView, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 0);
                else
                    popupWindow.showAtLocation(dialogView, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
                break;
            case FULLSCREEN:
                popupWindow.setHeight(WindowManager.LayoutParams.MATCH_PARENT);
                popupWindow.showAtLocation(dialogView, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                break;
            default:
                popupWindow.showAtLocation(dialogView, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
        }

        setMessageSeen();
    }

    private void setMessageSeen() {
        if (popupWindow.isShowing()) {
            String[] ids = {message.getMessageId()};
            MobileMessagingCore.getInstance(getActivity()).setMessagesSeen(ids);
        }
    }

    public void setVerticalScrollBar(int size) {
        webView.setVerticalScrollBarEnabled(true);
        webView.setScrollBarSize(size);
    }

    private void setActivityLifecycleListener(ActivityLifecycleListener listener) {
        final ActivityLifecycleMonitor activityLifecycleMonitor = MobileMessagingCore.getInstance(getActivity()).getActivityLifecycleMonitor();
        if (getActivity() != null && activityLifecycleMonitor != null)
            activityLifecycleMonitor.activityListener = listener;
    }

    private boolean isBanner() {
        return message.type == InAppWebViewMessage.InAppWebViewType.BANNER;
    }

    synchronized private void coreBroadcaster() {
        if (coreBroadcaster == null) {
            coreBroadcaster = new AndroidBroadcaster(getActivity());
        }
    }

    /*
     * This method converts dp(density point) unit to equivalent pixels, depending on device density.
     */
    private static int convertDpToPixel(float dp, Context context) {
        return Math.round(dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    public static class InAppWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            PAGE_LOAD_PROGRESS = newProgress;
            MobileMessagingLogger.d(TAG, "Page progress [" + PAGE_LOAD_PROGRESS + "%]");
            super.onProgressChanged(view, newProgress);
        }
    }

    private static void logWebViewError(int errorCode) {
        String message = null;
        String title = null;
        switch (errorCode) {
            case WebViewClient.ERROR_AUTHENTICATION:
                message = "User authentication failed on server";
                title = "Auth Error";
                break;
            case WebViewClient.ERROR_TIMEOUT:
                message = "The server is taking too much time to communicate. Try again later.";
                title = "Connection Timeout";
                break;
            case WebViewClient.ERROR_TOO_MANY_REQUESTS:
                message = "Too many requests during this load";
                title = "Too Many Requests";
                break;
            case WebViewClient.ERROR_UNKNOWN:
                message = "Generic error";
                title = "Unknown Error";
                break;
            case WebViewClient.ERROR_BAD_URL:
                message = "Check entered URL..";
                title = "Malformed URL";
                break;
            case WebViewClient.ERROR_CONNECT:
                message = "Failed to connect to the server";
                title = "Connection";
                break;
            case WebViewClient.ERROR_FAILED_SSL_HANDSHAKE:
                message = "Failed to perform SSL handshake";
                title = "SSL Handshake Failed";
                break;
            case WebViewClient.ERROR_HOST_LOOKUP:
                message = "Server or proxy hostname lookup failed";
                title = "Host Lookup Error";
                break;
            case WebViewClient.ERROR_PROXY_AUTHENTICATION:
                message = "User authentication failed on proxy";
                title = "Proxy Auth Error";
                break;
            case WebViewClient.ERROR_REDIRECT_LOOP:
                message = "Too many redirects";
                title = "Redirect Loop Error";
                break;
            case WebViewClient.ERROR_UNSUPPORTED_AUTH_SCHEME:
                message = "Unsupported authentication scheme (not basic or digest)";
                title = "Auth Scheme Error";
                break;
            case WebViewClient.ERROR_UNSUPPORTED_SCHEME:
                message = "Unsupported URI scheme";
                title = "URI Scheme Error";
                break;
            case WebViewClient.ERROR_FILE:
                message = "Generic file error";
                title = "File";
                break;
            case WebViewClient.ERROR_FILE_NOT_FOUND:
                message = "File not found";
                title = "File";
                break;
            case WebViewClient.ERROR_IO:
                message = "The server failed to communicate. Try again later.";
                title = "IO Error";
                break;
        }

        if (message != null) {
            MobileMessagingLogger.e(TAG, title + ": " + message);
        }
    }

    @SuppressLint("StaticFieldLeak")
    public class ConnectionTimeoutHandler extends AsyncTask<Void, Void, String> {

        private static final String PAGE_LOADED = "PAGE_LOADED";
        private static final String CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT";
        private static final long CONNECTION_TIMEOUT_UNIT = 20000L; //20 seconds
        private static final int PAGE_LOAD_MAX_PROGRESS = 100;

        private final long startTime;
        private Boolean loaded = false;

        public ConnectionTimeoutHandler() {
            startTime = Time.now();
            InAppWebViewDialog.PAGE_LOAD_PROGRESS = 0;
        }

        @Override
        protected void onPostExecute(String result) {
            if (CONNECTION_TIMEOUT.equalsIgnoreCase(result)) {
                logWebViewError(WebViewClient.ERROR_TIMEOUT);
                clearWebView();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            while (!loaded) {
                if (InAppWebViewDialog.PAGE_LOAD_PROGRESS != PAGE_LOAD_MAX_PROGRESS
                        && (Time.now() - startTime) > CONNECTION_TIMEOUT_UNIT) {
                    return CONNECTION_TIMEOUT;
                } else if (InAppWebViewDialog.PAGE_LOAD_PROGRESS == PAGE_LOAD_MAX_PROGRESS) {
                    loaded = true;
                }
            }
            return PAGE_LOADED;
        }
    }
}