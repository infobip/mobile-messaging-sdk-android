package org.infobip.mobile.messaging.chat.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
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
import android.widget.Toast;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.ConfigurationException;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.chat.InAppChatErrors;
import org.infobip.mobile.messaging.chat.InAppChatImpl;
import org.infobip.mobile.messaging.chat.R;
import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachment;
import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachmentHelper;
import org.infobip.mobile.messaging.chat.core.InAppChatClient;
import org.infobip.mobile.messaging.chat.core.InAppChatClientImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatEvent;
import org.infobip.mobile.messaging.chat.core.InAppChatMobileImpl;
import org.infobip.mobile.messaging.chat.attachments.InAppChatPermissionsHelper;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewClient;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewManager;
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.util.ResourceLoader;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InAppChatActivity extends AppCompatActivity implements InAppChatWebViewManager {

    private static final String IN_APP_CHAT_MOBILE_INTERFACE = "InAppChatMobile";
    private static final String RES_ID_IN_APP_CHAT_WIDGET_URI = "ib_inappchat_widget_uri";
    private static final int CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS = 500;
    private static final int CONTENT_SELECTION_INTENT_CODE = 100;
    private static final int CHAT_CAMERA_PERMISSIONS_REQUEST_CODE = 101;
    private static final int OPEN_SETTINGS_INTENT_CODE = 102;

    private boolean sendButtonIsColored;
    private WidgetInfo widgetInfo;

    /* View components */
    private WebView webView;
    private EditText editText;
    private ImageView btnSend;
    private ImageView btnSendAttachment;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private RelativeLayout relativeLayout;
    private TextView chatNotAvailableView;

    private InAppChatClient inAppChatClient;
    private InAppChatViewSettingsResolver inAppChatViewSettingsResolver;
    private Boolean shouldUseWidgetConfig = null;
    private boolean receiversRegistered = false;
    private boolean chatNotAvailableViewShown = false;
    private String widgetUri;
    private boolean isWebViewLoaded = false;
    private float chatNotAvailableViewHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        widgetUri = ResourceLoader.loadStringResourceByName(this, RES_ID_IN_APP_CHAT_WIDGET_URI);
        inAppChatViewSettingsResolver = new InAppChatViewSettingsResolver(this);
        setTheme(inAppChatViewSettingsResolver.getChatViewTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ib_activity_chat);

        initViews();
        updateViews();

        loadWebPage(widgetUri);

        registerReceivers();

        Boolean chatWidgetConfigSynced = InAppChatImpl.getIsChatWidgetConfigSynced();
        if (chatWidgetConfigSynced != null && !chatWidgetConfigSynced) {
            chatErrors.insertError(InAppChatErrors.CONFIG_SYNC_ERROR);
        }

    }

    @Override
    protected void onDestroy() {
        unregisterReceivers();
        super.onDestroy();
    }

    private void updateViews() {
        widgetInfo = prepareWidgetInfo();
        if (widgetInfo != null) {
            updateToolbarConfigs();
            @ColorInt int widgetPrimaryColor = Color.parseColor(widgetInfo.getPrimaryColor());
            btnSendAttachment.getDrawable().setColorFilter(widgetPrimaryColor, PorterDuff.Mode.SRC_ATOP);
        }
    }

    private WidgetInfo prepareWidgetInfo() {
        SharedPreferences prefs = PropertyHelper.getDefaultMMSharedPreferences(this);
        String widgetId = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID.getKey(), null);
        String widgetTitle = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE.getKey(), null);
        String widgetPrimaryColor = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.getKey(), null);
        String widgetBackgroundColor = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.getKey(), null);
        String maxUploadContentSizeStr = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE.getKey(), null);
        long maxUploadContentSize = InAppChatAttachment.DEFAULT_MAX_UPLOAD_CONTENT_SIZE;

        if (StringUtils.isNotBlank(maxUploadContentSizeStr)) {
            maxUploadContentSize = Long.parseLong(maxUploadContentSizeStr);
        }

        if (widgetId != null) {
            return new WidgetInfo(widgetId, widgetTitle, widgetPrimaryColor, widgetBackgroundColor, maxUploadContentSize);
        }
        return null;
    }

    private void initViews() {
        progressBar = findViewById(R.id.ib_pb_chat);
        relativeLayout = findViewById(R.id.ib_rl_send_message);
        chatNotAvailableView = findViewById(R.id.ib_tv_chat_not_connected);
        chatNotAvailableViewHeight = getResources().getDimension(R.dimen.chat_not_available_tv_height);
        initToolbar();
        initWebView();
        initTextBar();
        initSendButton();
        initAttachmentButton();
    }

    private void initToolbar() {
        toolbar = findViewById(R.id.ib_toolbar_chat);
        if (toolbar == null) return;
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar == null) {
            toolbar.setNavigationIcon(R.drawable.ic_menu_white);
            return;
        }
        supportActionBar.setDisplayHomeAsUpEnabled(true);
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

    private void updateToolbarConfigs() {
        ActionBar actionBar = getSupportActionBar();
        if (toolbar == null || actionBar == null) {
            return;
        }

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

        String title = inAppChatViewSettingsResolver.getChatViewTitle();
        if (StringUtils.isBlank(title)) {
            title = widgetInfo.getTitle();
        }
        actionBar.setTitle(title);
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
        btnSendAttachment.setEnabled(isEnabled);
        isWebViewLoaded = isEnabled;
    }

    @Override
    public void onJSError() {
        chatErrors.insertError(InAppChatErrors.JS_ERROR);
        webView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        relativeLayout.setVisibility(View.GONE);
    }

    @Override
    public void openAttachmentPreview(String url, String type, String caption) {
        Intent intent = new Intent(this, InAppChatAttachmentPreviewActivity.class);
        intent.putExtra(InAppChatAttachmentPreviewActivity.EXTRA_URL, url);
        intent.putExtra(InAppChatAttachmentPreviewActivity.EXTRA_TYPE, type);
        intent.putExtra(InAppChatAttachmentPreviewActivity.EXTRA_CAPTION, caption);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /*
    Errors handling
     */

    private static final String CHAT_SERVICE_ERROR = "12";
    private static final String CHAT_WIDGET_NOT_FOUND = "24";

    private final BroadcastReceiver broadcastEventsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (intent.hasExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY)) {
                    chatErrors.insertError(InAppChatErrors.INTERNET_CONNECTION_ERROR);
                } else {
                    chatErrors.removeError(InAppChatErrors.INTERNET_CONNECTION_ERROR);
                }
            } else if (action.equals(InAppChatEvent.CHAT_CONFIGURATION_SYNCED.getKey())) {
                chatErrors.removeError(InAppChatErrors.CONFIG_SYNC_ERROR);
            } else if (action.equals(Event.API_COMMUNICATION_ERROR.getKey()) && intent.hasExtra(BroadcastParameter.EXTRA_EXCEPTION)) {
                MobileMessagingError mobileMessagingError = (MobileMessagingError) intent.getSerializableExtra(BroadcastParameter.EXTRA_EXCEPTION);
                String errorCode = mobileMessagingError.getCode();
                if (errorCode.equals(CHAT_SERVICE_ERROR) || errorCode.equals(CHAT_WIDGET_NOT_FOUND)) {
                    chatErrors.insertError(InAppChatErrors.CONFIG_SYNC_ERROR);
                }
            }
        }
    };

    private InAppChatErrors chatErrors = new InAppChatErrors(new InAppChatErrors.OnChangeListener() {
        @Override
        public void onErrorsChange(Set<String> newErrors, String removedError, String insertedError) {

            if (removedError != null) {
                //reload webView if it wasn't loaded in case when internet connection appeared
                if (removedError.equals(InAppChatErrors.INTERNET_CONNECTION_ERROR) && !isWebViewLoaded) {
                    loadWebPage(widgetUri);
                }

                //update views configuration and reload webPage in case there was config sync error
                if (removedError.equals(InAppChatErrors.CONFIG_SYNC_ERROR)) {
                    updateViews();
                    loadWebPage(widgetUri);
                }
            }

            if (newErrors.isEmpty()) {
                hideChatNotAvailableView();
            } else {
                showChatNotAvailableView();
            }
        }
    });

    private void showChatNotAvailableView() {
        if (!chatNotAvailableViewShown) {
            chatNotAvailableView.animate().translationY(chatNotAvailableViewHeight).setDuration(CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS);
        }
        chatNotAvailableViewShown = true;
    }

    private void hideChatNotAvailableView() {
        if (chatNotAvailableViewShown) {
            chatNotAvailableView.animate().translationY(0).setDuration(CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS);
        }
        chatNotAvailableViewShown = false;
    }

    protected void registerReceivers() {
        if (!receiversRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            intentFilter.addAction(InAppChatEvent.CHAT_CONFIGURATION_SYNCED.getKey());
            intentFilter.addAction(Event.API_COMMUNICATION_ERROR.getKey());
            registerReceiver(broadcastEventsReceiver, intentFilter);
            receiversRegistered = true;
        }
    }

    protected void unregisterReceivers() {
        if (receiversRegistered) {
            unregisterReceiver(broadcastEventsReceiver);
            receiversRegistered = false;
        }
    }

    private void initAttachmentButton() {
        btnSendAttachment = findViewById(R.id.ib_btn_send_attachment);
        btnSendAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile();
            }
        });
    }

    private void chooseFile() {
        if (checkRequiredPermissions(getApplicationContext())) {
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, prepareIntentForChooser());
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, prepareInitialIntentsForChooser());
            startActivityForResult(chooserIntent, CONTENT_SELECTION_INTENT_CODE);
        } else {
            MobileMessagingLogger.e("[InAppChat] Permissions required for attachments not granted", new ConfigurationException(ConfigurationException.Reason.MISSING_REQUIRED_PERMISSION, Manifest.permission.CAMERA).getMessage());
        }
    }

    private Intent prepareIntentForChooser() {
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        return contentSelectionIntent;
    }

    private Intent[] prepareInitialIntentsForChooser() {
        List<Intent> intentsForChooser = new ArrayList<Intent>();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            intentsForChooser.add(takePictureIntent);
        }
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            intentsForChooser.add(takeVideoIntent);
        }
        Intent[] intentsArray = new Intent[intentsForChooser.size()];
        intentsForChooser.toArray(intentsArray);
        return intentsArray;
    }

    private boolean hasCamera() {
        return android.hardware.Camera.getNumberOfCameras() > 0;
    }

    private boolean checkRequiredPermissions(@NonNull Context context) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                !hasCamera() ||
                !InAppChatPermissionsHelper.hasPermissionInManifest(context, Manifest.permission.CAMERA))
            return false;

        final List<String> permissionsToAsk = new ArrayList<String>();
        final List<String> neverAskPermissions = new ArrayList<String>();
        checkPermission(Manifest.permission.CAMERA, permissionsToAsk, neverAskPermissions);

        if (neverAskPermissions.size() > 0) {
            showSettingsDialog(neverAskPermissions);
            return false;
        }
        String[] permissionsToAskArray = new String[permissionsToAsk.size()];
        permissionsToAsk.toArray(permissionsToAskArray);
        if (permissionsToAsk.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsToAskArray, CHAT_CAMERA_PERMISSIONS_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void checkPermission(String permission, final List<String> permissionsToAsk, final List<String> neverAskPermissions) {
        InAppChatPermissionsHelper.checkPermission(this, permission, new InAppChatPermissionsHelper.InAppChatPermissionAskListener() {
            @Override
            public void onNeedPermission(Context context, String permission) {
                permissionsToAsk.add(permission);
            }

            @Override
            public void onPermissionPreviouslyDeniedWithNeverAskAgain(Context context, String permission) {
                neverAskPermissions.add(permission);
            }

            @Override
            public void onPermissionGranted(Context context, String permission) {
            }
        });
    }

    private void showSettingsDialog(List<String> permissions) {
        //TODO: localize
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.ib_chat_permissions_not_granted_title);
        builder.setMessage(R.string.ib_chat_permissions_not_granted_message);
        builder.setPositiveButton(R.string.ib_chat_button_settings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }
        });
        builder.setNegativeButton(R.string.mm_button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, OPEN_SETTINGS_INTENT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == CONTENT_SELECTION_INTENT_CODE) {
            if (data != null) {

                InAppChatAttachmentHelper.makeAttachment(this, data, new InAppChatAttachmentHelper.InAppChatAttachmentHelperListener() {
                    @Override
                    public void onAttachmentCreated(final InAppChatAttachment attachment) {
                        if (attachment != null) {
                            MobileMessagingLogger.w("[InAppChat] Attachment created, will send Attachment");
                            inAppChatClient.sendChatMessage(null, attachment);
                        } else {
                            MobileMessagingLogger.e("[InAppChat] Can't create attachment");
                        }
                    }

                    @Override
                    public void onError(final Context context, InternalSdkError.InternalSdkException exception) {
                        MobileMessagingLogger.e("[InAppChat] Maximum allowed attachment size exceeded" + widgetInfo.getMaxUploadContentSize());
                        Toast.makeText(context, R.string.ib_chat_allowed_attachment_size_exceeded, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CHAT_CAMERA_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) { //Permission was granted.
                chooseFile();
            }
        }
    }
}
