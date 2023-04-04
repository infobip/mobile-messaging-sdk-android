package org.infobip.mobile.messaging.interactive.inapp.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import org.infobip.mobile.messaging.R;
import org.infobip.mobile.messaging.app.ActivityLifecycleMonitor;
import org.infobip.mobile.messaging.interactive.inapp.InAppWebViewMessage;
import org.infobip.mobile.messaging.interactive.inapp.InAppWebViewMessage.InAppWebViewPosition;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

public class InAppWebViewDialog extends ActivityLifecycleMonitor implements InAppWebView {
    private static final String TAG = "InAppWebViewDialog";
    private static final int DIALOG_TIMEOUT = 5000;
    private static final int CORNER_RADIUS = 20;
    public static final int WIDTH_PADDING = 16;
    public static final int VERTICAL_SCROLLBAR_SIZE = 30;
    private final Callback callback;
    private DisplayMetrics displayMetrics;
    private PopupWindow popupWindow;
    private WebView webView;
    private final ActivityWrapper activityWrapper;
    private boolean isMessageDisplayed = false;
    private InAppWebViewMessage message;

    InAppWebViewDialog(Callback callback, ActivityWrapper activityWrapper) {
        super(activityWrapper.getActivity().getApplication());
        this.callback = callback;
        this.activityWrapper = activityWrapper;
        displayMetrics = new DisplayMetrics();
        activityWrapper.getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
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
                webView.destroy();
                isMessageDisplayed = false;
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
            //TODO: Find a way how to display webview on screen rotation without recreating webview
            updatePageHeight(pageHeight);
        }
    }

    @Override
    public void show(@NonNull final InAppWebViewMessage message) {
        this.message = message;

        LayoutInflater inflater = activityWrapper.getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.ib_inapp_webview, null);
        CardView cardView = dialogView.findViewById(R.id.webview_dialog_card);
        cardView.setRadius(CORNER_RADIUS);

        popupWindow = new PopupWindow(dialogView, displayMetrics.widthPixels - WIDTH_PADDING, WindowManager.LayoutParams.WRAP_CONTENT, false);

        if (message.type == InAppWebViewMessage.InAppWebViewType.BANNER) {
            setDialogAnimations(message.position);
            popupWindow.setOutsideTouchable(false);
        } else {
            popupWindow.setOutsideTouchable(true);
        }

        setupWebViewForDisplaying(cardView);

        webView.loadUrl(message.url);

        setDialogTimeout(DIALOG_TIMEOUT);
        showWebViewDialog(dialogView, message.position, message.type);

        isMessageDisplayed = true;
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebViewForDisplaying(CardView dialogView) {
        webView = dialogView.findViewById(R.id.ib_webview);
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                MobileMessagingLogger.d(TAG, description);
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.addJavascriptInterface(new InAppWebViewInterface(), "InAppWebViewInterface");
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
            isMessageDisplayed = false;
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
        Window window = activityWrapper.getActivity().getWindow();

        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);
        int calculatedPageHeight = convertDpToPixel(pageHeight, activityWrapper.getActivity());

        if (message.type == InAppWebViewMessage.InAppWebViewType.FULLSCREEN && calculatedPageHeight > displayMetrics.heightPixels)
            setVerticalScrollBar(VERTICAL_SCROLLBAR_SIZE);

        try {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                if (message.type == InAppWebViewMessage.InAppWebViewType.BANNER) {
                    int minHeight = activityWrapper.getActivity().getResources().getDimensionPixelSize(R.dimen.dialog_banner_min_height);
                    int maxHeight = activityWrapper.getActivity().getResources().getDimensionPixelSize(R.dimen.dialog_banner_max_height);
                    int height = Math.min(maxHeight, Math.max(minHeight, calculatedPageHeight));
                    ViewGroup.LayoutParams viewFlowLayout = webView.getLayoutParams();
                    viewFlowLayout.height = height;
                    webView.setLayoutParams(viewFlowLayout);
                }
            });
        } catch (Exception e) {
            MobileMessagingLogger.w("Failed due to " + e.getMessage());
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (isMessageDisplayed)
            show(message);
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
    }

    public void setVerticalScrollBar(int size) {
        webView.setVerticalScrollBarEnabled(true);
        webView.setScrollBarSize(size);
    }

    /*
     * This method converts dp(density point) unit to equivalent pixels, depending on device density.
     */
    private static int convertDpToPixel(float dp, Context context) {
        return Math.round(dp * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}