package org.infobip.mobile.messaging.chat.view;

import static android.app.Activity.RESULT_OK;
import static org.infobip.mobile.messaging.chat.attachments.InAppChatAttachmentHelper.MIME_TYPE_VIDEO_MP_4;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

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
import org.infobip.mobile.messaging.chat.core.InAppChatClient;
import org.infobip.mobile.messaging.chat.core.InAppChatClientImpl;
import org.infobip.mobile.messaging.chat.core.InAppChatEvent;
import org.infobip.mobile.messaging.chat.core.InAppChatMultiThreadFlag;
import org.infobip.mobile.messaging.chat.core.InAppChatWebViewManager;
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.chat.utils.CommonUtils;
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.permissions.PermissionsRequestManager;
import org.infobip.mobile.messaging.util.StringUtils;
import org.infobip.mobile.messaging.util.SystemInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InAppChatFragment extends Fragment implements InAppChatWebViewManager, PermissionsRequestManager.PermissionsRequester {

    private static final int CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS = 500;
    private static final int CHAT_INPUT_VISIBILITY_ANIM_DURATION_MILLIS = 300;
    private static final int USER_INPUT_CHECKER_DELAY_MS = 250;

    private boolean sendButtonIsColored;
    private WidgetInfo widgetInfo;

    /* View components */
    private InAppChatWebView webView;
    private EditText messageInput;
    private ImageView sendMessageButton;
    private ImageView sendAttachmentButton;
    private ProgressBar spinner;
    private Toolbar toolbar;
    private LinearLayout msgInputWrapper;
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
    private Uri capturedVideoUri;
    private View containerView;
    private PermissionsRequestManager permissionsRequestManager;
    private boolean fragmentCouldBePaused = true;
    private boolean fragmentCouldBeResumed = true;
    private boolean isToolbarHidden = false;
    private boolean isInputControlsVisible = true;
    private boolean fragmentHidden = false;
    private LocalizationUtils localization;

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

        permissionsRequestManager = new PermissionsRequestManager(this, this);

        initViews();
        setControlsEnabled(false);
        updateViews();
    }

    private void localisation() {
        localization = LocalizationUtils.getInstance(requireContext());
        containerView.findViewById(R.id.ib_lc_iv_input_top_border).setContentDescription(localization.getString(R.string.ib_iv_input_border_desc));
        sendAttachmentButton.setContentDescription(localization.getString(R.string.ib_iv_btn_send_attachment_desc));
        sendMessageButton.setContentDescription(localization.getString(R.string.ib_iv_btn_send_desc));
        containerView.<TextView>findViewById(R.id.ib_lc_et_msg_input).setHint(localization.getString(R.string.ib_chat_message_hint));
        errorToast.setText(localization.getString(R.string.ib_chat_no_connection));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!fragmentHidden) {
            fragmentPaused();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!fragmentHidden) {
            fragmentResumed();
        }
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
        fragmentHidden = hidden;
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
        updateBackgroundColor();
    }

    private void updateBackgroundColor() {
        @ColorInt int backgroundColor = Color.parseColor(widgetInfo.getBackgroundColor());
        mainWindow.setBackgroundColor(backgroundColor);
        webView.setBackgroundColor(backgroundColor);
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
        String language = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE.getKey(), null);
        long maxUploadContentSize = InAppChatMobileAttachment.DEFAULT_MAX_UPLOAD_CONTENT_SIZE;

        if (StringUtils.isNotBlank(maxUploadContentSizeStr)) {
            maxUploadContentSize = Long.parseLong(maxUploadContentSizeStr);
        }

        if (widgetId != null) {
            return new WidgetInfo(widgetId, widgetTitle, widgetPrimaryColor, widgetBackgroundColor, maxUploadContentSize, language);
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
                    fillButtonByPrimaryColor(sendMessageButton);
                    sendButtonIsColored = true;
                } else if (s.length() == 0) {
                    sendMessageButton.getDrawable().clearColorFilter();
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
        sendMessageButton = containerView.findViewById(R.id.ib_lc_iv_send_btn);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
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
        applyLanguage();
    }

    private void applyLanguage() {
        String storedLanguage = widgetInfo.getLanguage();
        String language;
        if (storedLanguage == null) {
            language = MobileMessagingCore.getInstance(getContext()).getInstallation().getLanguage();
            LocalizationUtils.getInstance(getContext()).setLanguage(LocalizationUtils.localeFromString(language));
        } else {
            language = storedLanguage;
        }
        localisation();
        setLanguage(language);
    }

    @Override
    public void onPageFinished() {
        spinner.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    @Override
    public void setControlsEnabled(boolean isEnabled) {
        messageInput.setEnabled(isEnabled);
        sendMessageButton.setEnabled(isEnabled);
        sendAttachmentButton.setEnabled(isEnabled);
        isWebViewLoaded = isEnabled;
        if (isEnabled) InAppChatImpl.getInstance(getContext()).resetMessageCounter();
    }

    @Override
    public void onJSError() {
        chatErrors().insertError(InAppChatErrors.JS_ERROR);
        webView.setVisibility(View.GONE);
        spinner.setVisibility(View.GONE);
        setControlsEnabled(false);
    }

    @Override
    public void setControlsVisibility(boolean isVisible) {
        if (isInputControlsVisible == isVisible) {
            return;
        } else if (isVisible) {
            msgInputWrapper.animate().translationY(0).setDuration(CHAT_INPUT_VISIBILITY_ANIM_DURATION_MILLIS).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    msgInputWrapper.setVisibility(View.VISIBLE);
                }
            });
        } else {
            msgInputWrapper.animate().translationY(msgInputWrapper.getHeight()).setDuration(CHAT_INPUT_VISIBILITY_ANIM_DURATION_MILLIS).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    msgInputWrapper.setVisibility(View.GONE);
                }
            });
        }
        isInputControlsVisible = isVisible;
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

    @Override
    public void setLanguage(String language) {
        inAppChatClient.setLanguage(language);
    }

    @Override
    public void sendContextualMetaData(String data, InAppChatMultiThreadFlag multiThreadFlag) {
        inAppChatClient.sendContextualData(data, multiThreadFlag);
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

    private Uri getCapturedMediaUrl(Intent data) {
        Uri uri = (data != null) ? data.getData() : null;
        String mimeType = (uri != null) ? InAppChatMobileAttachment.getMimeType(getFragmentActivity(), data, uri) : null;
        return (mimeType != null && mimeType.equals(MIME_TYPE_VIDEO_MP_4)) ? capturedVideoUri : capturedImageUri;
    }

    private final ActivityResultLauncher<Intent> attachmentChooserLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        InAppChatAttachmentHelper.makeAttachment(getFragmentActivity(), data, getCapturedMediaUrl(data), new InAppChatAttachmentHelper.InAppChatAttachmentHelperListener() {
                            @Override
                            public void onAttachmentCreated(final InAppChatMobileAttachment attachment) {
                                if (attachment != null) {
                                    MobileMessagingLogger.w("[InAppChat] Attachment created, will send Attachment");
                                    inAppChatClient.sendChatMessage(null, attachment);
                                } else {
                                    MobileMessagingLogger.e("[InAppChat] Can't create attachment");
                                    Toast.makeText(getFragmentActivity(), R.string.ib_chat_cant_create_attachment, Toast.LENGTH_SHORT).show();
                                }
                                deleteEmptyMediaFiles();
                            }

                            @Override
                            public void onError(final Context context, InternalSdkError.InternalSdkException exception) {
                                if (exception.getMessage().equals(InternalSdkError.ERROR_ATTACHMENT_MAX_SIZE_EXCEEDED.get())) {
                                    MobileMessagingLogger.e("[InAppChat] Maximum allowed attachment size exceeded" + widgetInfo.getMaxUploadContentSize());
                                    Toast.makeText(context, R.string.ib_chat_allowed_attachment_size_exceeded, Toast.LENGTH_SHORT).show();
                                } else {
                                    MobileMessagingLogger.e("[InAppChat] Attachment content is not valid.");
                                    Toast.makeText(context, R.string.ib_chat_cant_create_attachment, Toast.LENGTH_SHORT).show();
                                }
                                deleteEmptyMediaFiles();
                            }
                        });
                    } else {
                        deleteEmptyMediaFiles();
                    }
                }
            }
    );

    private void deleteEmptyMediaFiles() {
        InAppChatAttachmentHelper.deleteEmptyFileByUri(getContext(), capturedImageUri);
        InAppChatAttachmentHelper.deleteEmptyFileByUri(getContext(), capturedVideoUri);
    }

    private void chooseFile() {
        fragmentCouldBePaused = false;
        if (!isRequiredPermissionsGranted()) {
            if (SystemInformation.isTiramisuOrAbove()) {
                MobileMessagingLogger.e("[InAppChat] Permissions required for attachments not granted", new ConfigurationException(ConfigurationException.Reason.MISSING_REQUIRED_PERMISSION,
                        Manifest.permission.CAMERA + ", " + Manifest.permission.READ_MEDIA_IMAGES + ", " + Manifest.permission.READ_MEDIA_VIDEO + ", " + Manifest.permission.READ_MEDIA_AUDIO + ", " + Manifest.permission.WRITE_EXTERNAL_STORAGE).getMessage());
            } else {
                MobileMessagingLogger.e("[InAppChat] Permissions required for attachments not granted", new ConfigurationException(ConfigurationException.Reason.MISSING_REQUIRED_PERMISSION, Manifest.permission.WRITE_EXTERNAL_STORAGE).getMessage());
            }
            return;
        }
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, prepareIntentForChooser());
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, prepareInitialIntentsForChooser());
        attachmentChooserLauncher.launch(chooserIntent);
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
        if (Build.VERSION.SDK_INT < 29) {
            capturedImageUri = InAppChatAttachmentHelper.getOutputImageUri(getFragmentActivity());
        } else {
            capturedImageUri = InAppChatAttachmentHelper.getOutputImageUrlAPI29(getFragmentActivity());
        }
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedImageUri);
        PackageManager packageManager = getPackageManager();
        if (packageManager != null && takePictureIntent.resolveActivity(packageManager) != null && capturedImageUri != null) {
            intentsForChooser.add(takePictureIntent);
        }
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (Build.VERSION.SDK_INT > 30) {
            capturedVideoUri = InAppChatAttachmentHelper.getOutputVideoUrl(getFragmentActivity());
            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, capturedVideoUri);
        }
        if (packageManager != null && takeVideoIntent.resolveActivity(packageManager) != null) {
            intentsForChooser.add(takeVideoIntent);
        }
        Intent[] intentsArray = new Intent[intentsForChooser.size()];
        intentsForChooser.toArray(intentsArray);
        return intentsArray;
    }

    /* PermissionsRequester */
    @NonNull
    @Override
    public String[] requiredPermissions() {
        if (SystemInformation.isTiramisuOrAbove()) {
            return new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO};
        } else return new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
    }

    @Override
    public boolean shouldShowPermissionsNotGrantedDialogIfShownOnce() {
        return true;
    }

    @Override
    public int permissionsNotGrantedDialogTitle() {
        return R.string.ib_chat_permissions_not_granted_title;
    }

    @Override
    public int permissionsNotGrantedDialogMessage() {
        return R.string.ib_chat_permissions_not_granted_message;
    }

    @Override
    public void onPermissionGranted() {
        chooseFile();
    }

    public boolean isRequiredPermissionsGranted() {
        if (getPackageManager() == null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
                android.hardware.Camera.getNumberOfCameras() == 0) {
            return false;
        }
        return permissionsRequestManager.isRequiredPermissionsGranted();
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