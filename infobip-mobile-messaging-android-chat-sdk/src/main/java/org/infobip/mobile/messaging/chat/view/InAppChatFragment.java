package org.infobip.mobile.messaging.chat.view;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.ConfigurationException;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.chat.InAppChat;
import org.infobip.mobile.messaging.chat.InAppChatErrors;
import org.infobip.mobile.messaging.chat.InAppChatImpl;
import org.infobip.mobile.messaging.chat.R;
import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachmentHelper;
import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment;
import org.infobip.mobile.messaging.chat.attachments.PermissionsRequestManager;
import org.infobip.mobile.messaging.chat.core.InAppChatClient;
import org.infobip.mobile.messaging.chat.core.InAppChatClientImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatEvent;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewManager;
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.chat.utils.CommonUtils;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.SoftwareInformation;
import org.infobip.mobile.messaging.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static android.app.Activity.RESULT_OK;

public class InAppChatFragment extends Fragment implements InAppChatWebViewManager, PermissionsRequestManager.PermissionsRequester {

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String OUTPUT_MEDIA_PATH = "/InAppChat";
    private static final int CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS = 500;
    private static final int CONTENT_SELECTION_INTENT_CODE = 100;
    private static final int USER_INPUT_CHECKER_DELAY_MS = 250;

    private boolean sendButtonIsColored;
    private WidgetInfo widgetInfo;

    /* View components */
    private InAppChatWebView webView;
    private EditText messageInput;
    private ImageView sendButton;
    private ImageView sendAttachmentButton;
    private ProgressBar spinner;
    private Toolbar toolbar;
    private RelativeLayout msgInputWrapper;
    private RelativeLayout mainWindow;
    private TextView errorToast;

    private InAppChatClient inAppChatClient;
    private InAppChatViewSettingsResolver inAppChatViewSettingsResolver;
    private final Handler inputCheckerHandler = new Handler(Looper.getMainLooper());
    private InAppChatInputFinishChecker inputFinishChecker;
    private Boolean shouldUseWidgetConfig = null;
    private boolean receiversRegistered = false;
    private boolean chatNotAvailableViewShown = false;
    private boolean isWebViewLoaded = false;
    private float chatNotAvailableViewHeight;
    private Uri capturedImageUri;
    private View containerView;
    private PermissionsRequestManager permissionsRequestManager;
    private boolean fragmentCouldBePaused = true;
    private boolean fragmentCouldBeResumed = true;
    private boolean isToolbarHidden = false;

    /**
     * Implement InAppChatActionBarProvider in your Activity, where InAppChatWebViewFragment will be added.
     */
    public interface InAppChatActionBarProvider {
        /**
         * Provide original ActionBar, to give in-app chat ability to hide it and use it's own ActionBar.
         * It will be hidden when in-app Chat fragment shown and returned back, when in-app Chat fragment hidden.
         */
        @Nullable
        ActionBar getOriginalSupportActionBar();

        /**
         * Implement back button behaviour.
         * <br>
         * Call following method with corresponding parameter:
         * <br>
         * {@link InAppChat#hideInAppChatFragment(FragmentManager)}
         */
        void onInAppChatBackPressed();
    }

    /* Lifecycle methods */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ib_fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        containerView = view;
        FragmentActivity fragmentActivity = getFragmentActivity();
        if (fragmentActivity == null) {
            MobileMessagingLogger.e("InAppChat", "Activity not exists in onViewCreated fragment method");
            return;
        }
        inAppChatViewSettingsResolver = new InAppChatViewSettingsResolver(fragmentActivity);
        fragmentActivity.setTheme(inAppChatViewSettingsResolver.getChatViewTheme());

        permissionsRequestManager = new PermissionsRequestManager(fragmentActivity, this);

        initViews();
        updateViews();
    }

    @Override
    public void onPause() {
        super.onPause();
        fragmentPaused();
    }

    @Override
    public void onResume() {
        super.onResume();
        fragmentResumed();
    }

    @Override
    public void onDestroy() {
        unregisterReceivers();
        mainWindow.removeView(webView);
        webView.removeAllViews();
        webView.destroy();
        super.onDestroy();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        if (!hidden) {
            initToolbar();
            updateViews();
            fragmentResumed();
        } else {
            fragmentPaused();
        }
        super.onHiddenChanged(hidden);
    }

    private void fragmentPaused() {
        if (!fragmentCouldBePaused) return;
        sendInputDraftImmediately();
        unregisterReceivers();
        hideChatNotAvailableView(0);
        webView.onPause();
        fragmentCouldBeResumed = true;
    }

    private void fragmentResumed() {
        fragmentCouldBePaused = true;
        if (!fragmentCouldBeResumed) return;
        registerReceivers();
        updateErrors();
        webView.onResume();
        loadWebPage(!isWebViewLoaded);
        syncInAppChatConfigIfNeeded();
        fragmentCouldBeResumed = false;
    }

    private void syncInAppChatConfigIfNeeded() {
        String pushRegistrationId = MobileMessagingCore.getInstance(getContext()).getPushRegistrationId();
        if (pushRegistrationId != null && widgetInfo == null) {
            ((MessageHandlerModule) InAppChat.getInstance(getContext())).performSyncActions();
        }
    }

    private void updateErrors() {
        chatErrors = null;

        Boolean chatWidgetConfigSynced = InAppChatImpl.getIsChatWidgetConfigSynced();
        if (chatWidgetConfigSynced != null && !chatWidgetConfigSynced) {
            chatErrors().insertError(InAppChatErrors.CONFIG_SYNC_ERROR);
        }
    }

    private void updateViews() {
        widgetInfo = prepareWidgetInfo();
        if (widgetInfo == null) return;

        updateToolbarConfigs();
        fillButtonByPrimaryColor(sendAttachmentButton);
    }

    private void sendInputDraftImmediately() {
        inputCheckerHandler.removeCallbacks(inputFinishChecker);
        inputCheckerHandler.post(inputFinishChecker);
    }

    private WidgetInfo prepareWidgetInfo() {
        SharedPreferences prefs = PropertyHelper.getDefaultMMSharedPreferences(getContext());
        String widgetId = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID.getKey(), null);
        String widgetTitle = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE.getKey(), null);
        String widgetPrimaryColor = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.getKey(), null);
        String widgetBackgroundColor = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.getKey(), null);
        String maxUploadContentSizeStr = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE.getKey(), null);
        long maxUploadContentSize = InAppChatMobileAttachment.DEFAULT_MAX_UPLOAD_CONTENT_SIZE;

        if (StringUtils.isNotBlank(maxUploadContentSizeStr)) {
            maxUploadContentSize = Long.parseLong(maxUploadContentSizeStr);
        }

        if (widgetId != null) {
            return new WidgetInfo(widgetId, widgetTitle, widgetPrimaryColor, widgetBackgroundColor, maxUploadContentSize);
        }
        return null;
    }

    private void initViews() {
        spinner = containerView.findViewById(R.id.ib_lc_pb_spinner);
        msgInputWrapper = containerView.findViewById(R.id.ib_lc_rl_msg_input_wrapper);
        mainWindow = containerView.findViewById(R.id.ib_lc_rl_main_window);
        errorToast = containerView.findViewById(R.id.ib_lc_tv_error_toast);
        chatNotAvailableViewHeight = getResources().getDimension(R.dimen.chat_not_available_tv_height);
        initToolbar();
        initWebView();
        initTextBar();
        initSendButton();
        initAttachmentButton();
    }

    private void initToolbar() {
        toolbar = containerView.findViewById(R.id.ib_toolbar_chat_fragment);
        if (toolbar == null) return;

        if (isToolbarHidden) {
            toolbar.setVisibility(View.GONE);
            return;
        }
        //If Activity has it's own ActionBar, it should be hidden.
        try {
            InAppChatActionBarProvider actionBarProvider = (InAppChatActionBarProvider) getFragmentActivity();
            if (actionBarProvider != null) {
                ActionBar ab = actionBarProvider.getOriginalSupportActionBar();
                if (ab != null) {
                    ab.hide();
                }
            }
        } catch (Exception e) {
            MobileMessagingLogger.e("[InAppChat] can't get actionBarProvider", e);
        }
        toolbar.setNavigationIcon(R.drawable.ic_chat_arrow_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeChatPage();
            }
        });
    }

    public void closeChatPage() {
        try {
            InAppChatActionBarProvider actionBarProvider = (InAppChatActionBarProvider) getFragmentActivity();
            if (actionBarProvider != null) {
                ActionBar ab = actionBarProvider.getOriginalSupportActionBar();
                if (ab != null) {
                    ab.show();
                }
                actionBarProvider.onInAppChatBackPressed();
            }
        } catch (Exception e) {
            MobileMessagingLogger.e("[InAppChat] can't get actionBarProvider", e);
        }
    }

    private void initTextBar() {
        messageInput = containerView.findViewById(R.id.ib_lc_et_msg_input);
        inputFinishChecker = new InAppChatInputFinishChecker(inAppChatClient);

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputCheckerHandler.removeCallbacks(inputFinishChecker);
                if (s.length() > 0 && !sendButtonIsColored) {
                    fillButtonByPrimaryColor(sendButton);
                    sendButtonIsColored = true;
                } else if (s.length() == 0) {
                    sendButton.getDrawable().clearColorFilter();
                    sendButtonIsColored = false;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                inputFinishChecker.setInputValue(s.toString());
                inputCheckerHandler.postDelayed(inputFinishChecker, USER_INPUT_CHECKER_DELAY_MS);
            }
        });
        messageInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                FragmentActivity activity = getFragmentActivity();
                if (activity != null && !hasFocus) {
                    InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        });
    }

    private void fillButtonByPrimaryColor(ImageView buttonToFill) {
        @ColorInt int widgetPrimaryColor = Color.parseColor(widgetInfo.getPrimaryColor());
        if (!shouldUseWidgetConfig()) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getTheme();
            if (theme != null) {
                theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
                widgetPrimaryColor = typedValue.data;
            }
        }
        buttonToFill.getDrawable().setColorFilter(widgetPrimaryColor, PorterDuff.Mode.SRC_ATOP);
    }

    private void updateToolbarConfigs() {
        if (toolbar == null || widgetInfo == null || isToolbarHidden) {
            return;
        }

        @ColorInt int primaryColor = Color.parseColor(widgetInfo.getPrimaryColor());
        @ColorInt int titleTextColor = Color.parseColor(widgetInfo.getBackgroundColor());
        @ColorInt int navigationIconColor = titleTextColor;
        @ColorInt int primaryDarkColor = calculatePrimaryDarkColor(primaryColor);

        // setup colors (from widget or local config)
        if (!shouldUseWidgetConfig()) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getTheme();
            if (theme != null) {
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
        }

        setStatusBarColor(primaryDarkColor);

        // set colors to views
        try {
            spinner.getIndeterminateDrawable().setColorFilter(primaryDarkColor, PorterDuff.Mode.SRC_IN);
        } catch (Exception ignored) {
        }
        toolbar.setBackgroundColor(primaryColor);

        String title = inAppChatViewSettingsResolver.getChatViewTitle();
        if (StringUtils.isBlank(title)) {
            title = widgetInfo.getTitle();
        }
        toolbar.setTitle(title);
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
            FragmentActivity activity = getFragmentActivity();
            if (activity == null) {
                return;
            }
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(statusBarColor);
        }
    }

    private boolean shouldUseWidgetConfig() {
        if (shouldUseWidgetConfig != null) return shouldUseWidgetConfig;

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getTheme();
        if (theme == null) {
            return true;
        }

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
        webView = containerView.findViewById(R.id.ib_lc_wv_in_app_chat);
        webView.setup(this);
        inAppChatClient = new InAppChatClientImpl(webView);
    }

    private void initSendButton() {
        sendButton = containerView.findViewById(R.id.ib_lc_iv_send_btn);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Editable text = messageInput.getText();
                if (text != null) {
                    inAppChatClient.sendChatMessage(CommonUtils.escapeJsonString(text.toString()));
                    text.clear();
                }
            }
        });
    }

    private void loadWebPage(Boolean force) {
        if (webView == null) return;
        webView.loadWebPage(force, widgetInfo);
    }

    @Override
    public void onPageStarted() {
        spinner.setVisibility(View.VISIBLE);
        webView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPageFinished() {
        spinner.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    @Override
    public void setControlsEnabled(boolean isEnabled) {
        messageInput.setEnabled(isEnabled);
        sendButton.setEnabled(isEnabled);
        sendAttachmentButton.setEnabled(isEnabled);
        isWebViewLoaded = isEnabled;
    }

    @Override
    public void onJSError() {
        chatErrors().insertError(InAppChatErrors.JS_ERROR);
        webView.setVisibility(View.GONE);
        spinner.setVisibility(View.GONE);
        msgInputWrapper.setVisibility(View.GONE);
    }

    @Override
    public void openAttachmentPreview(String url, String type, String caption) {
        fragmentCouldBePaused = false;
        Intent intent = new Intent(getContext(), InAppChatAttachmentPreviewActivity.class);
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
                    chatErrors().insertError(InAppChatErrors.INTERNET_CONNECTION_ERROR);
                } else {
                    chatErrors().removeError(InAppChatErrors.INTERNET_CONNECTION_ERROR);
                }
            } else if (action.equals(InAppChatEvent.CHAT_CONFIGURATION_SYNCED.getKey())) {
                chatErrors().removeError(InAppChatErrors.CONFIG_SYNC_ERROR);
            } else if (action.equals(Event.API_COMMUNICATION_ERROR.getKey()) && intent.hasExtra(BroadcastParameter.EXTRA_EXCEPTION)) {
                MobileMessagingError mobileMessagingError = (MobileMessagingError) intent.getSerializableExtra(BroadcastParameter.EXTRA_EXCEPTION);
                String errorCode = mobileMessagingError.getCode();
                if (errorCode.equals(CHAT_SERVICE_ERROR) || errorCode.equals(CHAT_WIDGET_NOT_FOUND)) {
                    chatErrors().insertError(InAppChatErrors.CONFIG_SYNC_ERROR);
                }
            } else if (action.equals(Event.REGISTRATION_CREATED.getKey())) {
                syncInAppChatConfigIfNeeded();
            }
        }
    };

    private InAppChatErrors chatErrors = null;

    private InAppChatErrors chatErrors() {
        if (chatErrors == null) {
            chatErrors = new InAppChatErrors(new InAppChatErrors.OnChangeListener() {
                @Override
                public void onErrorsChange(Set<String> newErrors, String removedError, String insertedError) {

                    if (removedError != null) {
                        //reload webView if it wasn't loaded in case when internet connection appeared
                        if (removedError.equals(InAppChatErrors.INTERNET_CONNECTION_ERROR) && !isWebViewLoaded) {
                            loadWebPage(true);
                        }

                        //update views configuration and reload webPage in case there was config sync error
                        if (removedError.equals(InAppChatErrors.CONFIG_SYNC_ERROR)) {
                            updateViews();
                            loadWebPage(true);
                        }
                    }

                    if (newErrors.isEmpty()) {
                        hideChatNotAvailableView(CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS);
                    } else {
                        showChatNotAvailableView(CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS);
                    }
                }
            });
        }
        return chatErrors;
    }

    private void showChatNotAvailableView(int duration) {
        if (!chatNotAvailableViewShown) {
            errorToast.setVisibility(View.VISIBLE);
            errorToast.animate().translationY(chatNotAvailableViewHeight).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    errorToast.setVisibility(View.VISIBLE);
                }
            });
        }
        chatNotAvailableViewShown = true;
    }

    private void hideChatNotAvailableView(int duration) {
        if (chatNotAvailableViewShown) {
            errorToast.animate().translationY(0).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    errorToast.setVisibility(View.INVISIBLE);
                }
            });
        }
        chatNotAvailableViewShown = false;
    }

    protected void registerReceivers() {
        if (!receiversRegistered) {
            FragmentActivity activity = getFragmentActivity();
            if (activity == null) {
                MobileMessagingLogger.e("InAppChat", "Can't register receivers");
                return;
            }
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            intentFilter.addAction(InAppChatEvent.CHAT_CONFIGURATION_SYNCED.getKey());
            intentFilter.addAction(Event.API_COMMUNICATION_ERROR.getKey());
            intentFilter.addAction(Event.REGISTRATION_CREATED.getKey());
            activity.registerReceiver(broadcastEventsReceiver, intentFilter);
            receiversRegistered = true;
        }
    }

    protected void unregisterReceivers() {
        if (receiversRegistered) {
            FragmentActivity activity = getFragmentActivity();
            if (activity == null) {
                MobileMessagingLogger.e("InAppChat", "Can't unregister receivers");
                return;
            }
            activity.unregisterReceiver(broadcastEventsReceiver);
            receiversRegistered = false;
        }
    }

    private void initAttachmentButton() {
        sendAttachmentButton = containerView.findViewById(R.id.ib_lc_iv_send_attachment_btn);
        sendAttachmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseFile();
            }
        });
    }

    private void chooseFile() {
        fragmentCouldBePaused = false;
        if (!isRequiredPermissionsGranted()) {
            MobileMessagingLogger.e("[InAppChat]", new ConfigurationException(ConfigurationException.Reason.MISSING_REQUIRED_PERMISSION, Manifest.permission.CAMERA + ", " + Manifest.permission.WRITE_EXTERNAL_STORAGE).getMessage());
            return;
        }
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, prepareIntentForChooser());
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, prepareInitialIntentsForChooser());
        startActivityForResult(chooserIntent, CONTENT_SELECTION_INTENT_CODE);
    }

    private Intent prepareIntentForChooser() {
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        return contentSelectionIntent;
    }

    private Intent[] prepareInitialIntentsForChooser() {
        List<Intent> intentsForChooser = new ArrayList<>();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        capturedImageUri = getOutputMediaUri();
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
        PackageManager packageManager = getPackageManager();
        if (packageManager != null && takePictureIntent.resolveActivity(packageManager) != null && capturedImageUri != null) {
            intentsForChooser.add(takePictureIntent);
        }
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (packageManager != null && takeVideoIntent.resolveActivity(packageManager) != null) {
            intentsForChooser.add(takeVideoIntent);
        }
        Intent[] intentsArray = new Intent[intentsForChooser.size()];
        intentsForChooser.toArray(intentsArray);
        return intentsArray;
    }

    @Nullable
    private Uri getOutputMediaUri() {
        FragmentActivity activity = getFragmentActivity();
        if (activity == null) {
            return null;
        }
        String appName = SoftwareInformation.getAppName(activity.getApplicationContext());
        File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File outputPicturesDirectory = new File(picturesDirectory.getPath() + File.separator + appName + OUTPUT_MEDIA_PATH);
        if (!outputPicturesDirectory.exists() && !outputPicturesDirectory.mkdirs()) {
            return null;
        }

        String fileName = JPEG_FILE_PREFIX + DateTimeUtil.dateToYMDHMSString(new Date()) + JPEG_FILE_SUFFIX;
        File pictureFile = new File(outputPicturesDirectory.getPath() + File.separator + fileName);

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, pictureFile.getPath());
        return activity.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK && requestCode == CONTENT_SELECTION_INTENT_CODE) {
            InAppChatAttachmentHelper.makeAttachment(getActivity(), data, getUriFromMediaStoreURI(capturedImageUri), new InAppChatAttachmentHelper.InAppChatAttachmentHelperListener() {
                @Override
                public void onAttachmentCreated(final InAppChatMobileAttachment attachment) {
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
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Nullable
    private Uri getUriFromMediaStoreURI(Uri mediaStoreUri) {
        FragmentActivity activity = getFragmentActivity();
        if (activity == null || mediaStoreUri == null) {
            return null;
        }
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.managedQuery(mediaStoreUri, proj, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return Uri.fromFile(new File(cursor.getString(column_index)));
    }

    /* PermissionsRequester */

    @NonNull
    @Override
    public String[] requiredPermissions() {
        return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
    }

    @Override
    public void onPermissionGranted() {
        chooseFile();
    }

    public boolean isRequiredPermissionsGranted() {
        if (getPackageManager() == null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
                android.hardware.Camera.getNumberOfCameras() == 0) {
            return false;
        }
        return permissionsRequestManager.isRequiredPermissionsGranted();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsRequestManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /* PermissionsRequester endregion */

    /* Helper methods to get FragmentActivity properties */
    @Nullable
    private FragmentActivity getFragmentActivity() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            MobileMessagingLogger.e("InAppChat", "Can't get fragment activity");
        }
        return activity;
    }

    @Nullable
    private PackageManager getPackageManager() {
        PackageManager pm = (getFragmentActivity() != null) ? getFragmentActivity().getPackageManager() : null;
        if (pm == null) {
            MobileMessagingLogger.e("InAppChat", "Can't get fragment activity package manager");
        }
        return pm;
    }

    @Nullable
    private Resources.Theme getTheme() {
        Resources.Theme theme = (getFragmentActivity() != null) ? getFragmentActivity().getTheme() : null;
        if (theme == null) {
            MobileMessagingLogger.e("InAppChat", "Can't get fragment activity theme");
        }
        return theme;
    }

    public void setIsToolbarHidden(Boolean hidden) {
        isToolbarHidden = hidden;
    }
}