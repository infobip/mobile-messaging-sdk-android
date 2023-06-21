package org.infobip.mobile.messaging.interactive.inapp.view;

import static android.content.Context.WINDOW_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
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
import org.infobip.mobile.messaging.R;
import org.infobip.mobile.messaging.app.ActivityLifecycleListener;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.interactive.inapp.InAppWebViewMessage;
import org.infobip.mobile.messaging.interactive.inapp.InAppWebViewMessage.InAppWebViewPosition;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.platform.Time;

public class InAppWebViewDialog implements InAppWebView, ActivityLifecycleListener {
    private static final String TAG = "[InAppWebViewDialog]";
    private static final int DIALOG_TIMEOUT = 10000;//10 seconds
    private static final int CORNER_RADIUS = 20;
    public static final int WIDTH_PADDING = 16;
    public static final int VERTICAL_SCROLLBAR_SIZE = 30;
    public static final double SCREEN_COVER_PERCENTAGE = 0.85;
    public static final int SCREEN_MARGINS = 16;
    private final Callback callback;
    private final DisplayMetrics displayMetrics;
    private PopupWindow popupWindow;
    private WebView webView;
    private final ActivityWrapper activityWrapper;
    private InAppWebViewMessage message;
    static int PAGE_LOAD_PROGRESS = 0;
    ConnectionTimeoutHandler timeoutHandler = null;
    View dialogView;
    CardView cardView;


    InAppWebViewDialog(Callback callback, ActivityWrapper activityWrapper) {
        this.callback = callback;
        this.activityWrapper = activityWrapper;
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
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
         * Dismiss WebView on Close button click
         */
        @JavascriptInterface
        public void dismissWebView() {
            MobileMessagingLogger.d(TAG, "onDismissWebView()");
            new Handler(Looper.getMainLooper()).post(() -> {
                popupWindow.dismiss();
                setActivityLifecycleListener(null);
                webView.destroy();
            });
        }

        /**
         * Handling WebView button clicks
         */
        @JavascriptInterface
        public void onButtonClick() {
            MobileMessagingLogger.d(TAG, "onButtonClick()");
        }


        /**
         * Handling WebView height change
         */
        @JavascriptInterface
        public void onHeightChanged(float pageHeight) {
            MobileMessagingLogger.d(TAG, "onHeightChanged: " + pageHeight);
            updatePageHeight(pageHeight);
        }
    }

    @Override
    public void show(@NonNull final InAppWebViewMessage message) {
        this.message = message;

        setActivityLifecycleListener(this);

        try {
            getActivity().runOnUiThread(() -> {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                dialogView = inflater.inflate(R.layout.ib_inapp_webview, null, false);

                cardView = dialogView.findViewById(R.id.webview_dialog_card);
                cardView.setRadius(CORNER_RADIUS);

                popupWindow = new PopupWindow(dialogView, displayMetrics.widthPixels - WIDTH_PADDING, WindowManager.LayoutParams.WRAP_CONTENT, false);


                int screenRotation = ((WindowManager) getActivity().getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                if (screenRotation == Surface.ROTATION_90 || screenRotation == Surface.ROTATION_270) { //Landscape
                    cardView.getLayoutParams().width = displayMetrics.heightPixels - WIDTH_PADDING;
                }

                popupWindow.setContentView(dialogView);

                if (message.type == InAppWebViewMessage.InAppWebViewType.BANNER) {
                    setDialogAnimations(message.position);
                    popupWindow.setOutsideTouchable(false);
                } else {
                    popupWindow.setOutsideTouchable(true);
                }
                setupWebViewForDisplaying(cardView);
                webView.loadUrl(message.url);
            });
        } catch (Exception e) {
            MobileMessagingLogger.e(TAG, "Failed to display webview for message with ID " + message.getMessageId() + " due to: " + e.getMessage());
        }
    }

    private Activity getActivity() {
        return activityWrapper.getActivity();
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebViewForDisplaying(CardView dialogView) {
        webView = dialogView.findViewById(R.id.ib_webview);
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
                setDialogTimeout(DIALOG_TIMEOUT);
                showWebViewDialog(dialogView, message.position, message.type);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                showError(errorCode);
                clearWebView();
            }
        });
        webView.setWebChromeClient(new InAppWebChromeClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.addJavascriptInterface(new InAppWebViewInterface(), "InAppWebViewInterface");
    }

    private void clearWebView() {
        try {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                webView.clearHistory();
                webView.clearCache(true);
                MobileMessagingLogger.d(TAG, "Deleted local history");
            });
        } catch (Exception e) {
            MobileMessagingLogger.e(TAG, "Failed to delete local history due to " + e.getMessage());
        }
        webView.clearFormData();
        webView.clearMatches();
        webView.destroy();
        webView.setVisibility(View.GONE);
        if (popupWindow.isShowing())
            popupWindow.dismiss();
    }

    // Hide after some seconds
    public void setDialogTimeout(int dialogTimeout) {
        final Handler handler = new Handler();
        final Runnable runnable = () -> {
            if (popupWindow.isShowing()) {
                popupWindow.dismiss();
            }
        };
        popupWindow.setOnDismissListener(() -> {
            callback.dismissed(this);
            handler.removeCallbacks(runnable);
        });
        handler.postDelayed(runnable, dialogTimeout);
    }

    private void setDialogAnimations(InAppWebViewPosition position) {
        if (position == InAppWebViewPosition.TOP)
            popupWindow.setAnimationStyle(R.style.BannerAnimationTop);
        else
            popupWindow.setAnimationStyle(R.style.BannerAnimationBottom);
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

    private static void showError(int errorCode) {
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

        private final long startTime;
        private Boolean loaded = false;

        public ConnectionTimeoutHandler() {
            startTime = Time.now();
            InAppWebViewDialog.PAGE_LOAD_PROGRESS = 0;
        }

        @Override
        protected void onPostExecute(String result) {
            if (CONNECTION_TIMEOUT.equalsIgnoreCase(result)) {
                showError(WebViewClient.ERROR_TIMEOUT);
                clearWebView();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            while (!loaded) {
                if (InAppWebViewDialog.PAGE_LOAD_PROGRESS != 100
                        && (Time.now() - startTime) > CONNECTION_TIMEOUT_UNIT) {
                    return CONNECTION_TIMEOUT;
                } else if (InAppWebViewDialog.PAGE_LOAD_PROGRESS == 100) {
                    loaded = true;
                }
            }
            return PAGE_LOADED;
        }
    }
}