package org.infobip.mobile.messaging.chat.view;

import static android.app.Activity.RESULT_OK;
import static org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient.ErrorCode.API_IO_ERROR;
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
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

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
import org.infobip.mobile.messaging.chat.core.InAppChatWidgetView;
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.chat.utils.ViewUtilsKt;
import org.infobip.mobile.messaging.chat.utils.CommonUtils;
import org.infobip.mobile.messaging.chat.utils.LocalizationUtils;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatToolbarStyle;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatToolbarStyleKt;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.InternalSdkError;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.permissions.PermissionsRequestManager;
import org.infobip.mobile.messaging.util.StringUtils;
import org.infobip.mobile.messaging.util.SystemInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InAppChatFragment extends Fragment implements InAppChatWebViewManager, PermissionsRequestManager.PermissionsRequester {

    private static final int CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS = 500;
    private static final int USER_INPUT_CHECKER_DELAY_MS = 250;

    private WidgetInfo widgetInfo;

    /* View components */
    private InAppChatWebView webView;
    private ProgressBar spinner;
    private MaterialToolbar toolbar;
    private RelativeLayout mainWindow;
    private TextView noConnectionErrorToast;
    private InAppChatInputView inAppChatInputView;

    private InAppChatClient inAppChatClient;
    private InAppChatThemeResolver inAppChatThemeResolver;
    private final Handler inputCheckerHandler = new Handler(Looper.getMainLooper());
    private InAppChatInputFinishChecker inputFinishChecker;
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
    private boolean isInputControlsVisible = false;
    private boolean fragmentHidden = false;
    private InAppChatWidgetView currentWidgetView = null;
    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            navigateBack();
        }
    };
    private InAppChatActionBarProvider inAppChatActionBarProvider;
    private MobileMessagingCore mobileMessagingCore;
    private LocalizationUtils localizationUtils;
    @ColorInt
    private int originalStatusBarColor;

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
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(requireContext(), InAppChatThemeResolver.getChatViewTheme(requireContext()));
        return inflater.cloneInContext(contextThemeWrapper).inflate(R.layout.ib_fragment_chat, container, false);
    }

    @Nullable
    @Override
    public Context getContext() {
        Context context = super.getContext();
        return new ContextThemeWrapper(context, InAppChatThemeResolver.getChatViewTheme(context));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        localizationUtils = LocalizationUtils.getInstance(requireContext());
        mobileMessagingCore = MobileMessagingCore.getInstance(getContext());
        containerView = view;
        permissionsRequestManager = new PermissionsRequestManager(this, this);

        initViews();
        setControlsEnabled(false);
        updateViews();
        initBackPressHandler();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeBackPressHandler();
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
        backPressedCallback.setEnabled(!hidden);
        super.onHiddenChanged(hidden);
    }

    private void fragmentPaused() {
        if (!fragmentCouldBePaused) return;
        sendInputDraftImmediately();
        unregisterReceivers();
        hideNoInternetConnectionView(0);
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

        Result<WidgetInfo, MobileMessagingError> chatWidgetConfigSyncResult = InAppChatImpl.getChatWidgetConfigSyncResult();
        if (chatWidgetConfigSyncResult != null && !chatWidgetConfigSyncResult.isSuccess()) {
            MobileMessagingError error = chatWidgetConfigSyncResult.getError();
            boolean isInternetConnectionError = API_IO_ERROR.getValue().equals(error.getCode()) && error.getType() == MobileMessagingError.Type.SERVER_ERROR;
            boolean isRegistrationPendingError = InternalSdkError.NO_VALID_REGISTRATION.getError().getCode().equals(error.getCode()) && mobileMessagingCore.isRegistrationIdReported();
            //connection error handled separately by broadcast receiver, sync is triggered again after registration, do not show error
            if (!isInternetConnectionError && !isRegistrationPendingError) {
                chatErrors().insertError(new InAppChatErrors.Error(InAppChatErrors.CONFIG_SYNC_ERROR, error.getMessage()));
            }
        }
    }

    private void updateViews() {
        widgetInfo = prepareWidgetInfo();
        if (widgetInfo == null) return;

        applyToolbarStyle();
        applyInAppChatInputStyle();
        applyInAppChatStyle();
        if (!isMultiThread()) {
            setInputVisibility(true);
        }
    }

    private void applyInAppChatStyle() {
        InAppChatStyle style = InAppChatStyle.create(requireContext(), widgetInfo);
        mainWindow.setBackgroundColor(style.getBackgroundColor());
        webView.setBackgroundColor(style.getBackgroundColor());
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
        boolean widgetMultiThread = prefs.getBoolean(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTITHREAD.getKey(), false);
        String language = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE.getKey(), null);
        long maxUploadContentSize = InAppChatMobileAttachment.DEFAULT_MAX_UPLOAD_CONTENT_SIZE;

        if (StringUtils.isNotBlank(maxUploadContentSizeStr)) {
            maxUploadContentSize = Long.parseLong(maxUploadContentSizeStr);
        }

        if (widgetId != null) {
            return new WidgetInfo(widgetId, widgetTitle, widgetPrimaryColor, widgetBackgroundColor, maxUploadContentSize, language, widgetMultiThread);
        }
        return null;
    }

    private void initViews() {
        spinner = containerView.findViewById(R.id.ib_lc_pb_spinner);
        inAppChatInputView = containerView.findViewById(R.id.ib_lc_msg_input);
        mainWindow = containerView.findViewById(R.id.ib_lc_rl_main_window);
        noConnectionErrorToast = containerView.findViewById(R.id.ib_lc_tv_error_toast);
        chatNotAvailableViewHeight = getResources().getDimension(R.dimen.chat_not_available_tv_height);
        initToolbar();
        initWebView();
        initInAppChatInputView();
    }

    //region Toolbar
    private void initToolbar() {
        toolbar = containerView.findViewById(R.id.ib_lc_chat_tb);
        if (toolbar == null) return;

        //If Activity has it's own ActionBar, it should be hidden.
        try {
            InAppChatActionBarProvider actionBarProvider = getInAppChatActionBarProvider();
            if (actionBarProvider != null) {
                ActionBar ab = actionBarProvider.getOriginalSupportActionBar();
                if (ab != null) {
                    ab.hide();
                }
            }
        } catch (Exception e) {
            MobileMessagingLogger.e("[InAppChat] can't get actionBarProvider", e);
        }
        toolbar.setNavigationOnClickListener(view -> navigateBack());
    }

    private void navigateBack() {
        inAppChatInputView.hideKeyboard();
        if (isMultiThread() && currentWidgetView != null) {
            switch (currentWidgetView) {
                case LOADING:
                case THREAD_LIST:
                case SINGLE_MODE_THREAD:
                    closeChatPage();
                    break;
                case THREAD:
                case LOADING_THREAD:
                case CLOSED_THREAD:
                    showThreadList();
                    break;
            }
        } else {
            closeChatPage();
        }
    }

    private void closeChatPage() {
        try {
            InAppChatActionBarProvider actionBarProvider = getInAppChatActionBarProvider();
            if (actionBarProvider != null) {
                ActionBar ab = actionBarProvider.getOriginalSupportActionBar();
                if (ab != null) {
                    ab.show();
                }
                backPressedCallback.setEnabled(false); //when InAppChat is used as Activity need to disable callback before onBackPressed() is called to avoid endless loop
                actionBarProvider.onInAppChatBackPressed();
            }
            if (originalStatusBarColor != 0)
                ViewUtilsKt.setStatusBarColor(getFragmentActivity(), originalStatusBarColor);
        } catch (Exception e) {
            MobileMessagingLogger.e("[InAppChat] can't get actionBarProvider", e);
        }
    }

    private void showThreadList() {
        inAppChatClient.showThreadList();
    }

    private void applyToolbarStyle() {
        if (toolbar == null || widgetInfo == null) {
            return;
        }

        InAppChatToolbarStyle style = InAppChatToolbarStyle.createChatToolbarStyle(requireContext(), widgetInfo);
        toolbar.setNavigationIcon(style.getNavigationIcon());

        this.originalStatusBarColor = ViewUtilsKt.getStatusBarColor(getFragmentActivity());
        ViewUtilsKt.setStatusBarColor(getFragmentActivity(), style.getStatusBarBackgroundColor());
        try {
            spinner.getIndeterminateDrawable().setColorFilter(style.getStatusBarBackgroundColor(), PorterDuff.Mode.SRC_IN);
        } catch (Exception ignored) {
        }

        InAppChatToolbarStyleKt.apply(style, toolbar);
    }
    //endregion

    //region InAppChatInputView
    private void initInAppChatInputView() {
        initTextBar();
        initSendButton();
        initAttachmentButton();
    }

    private void initTextBar() {
        inputFinishChecker = new InAppChatInputFinishChecker(inAppChatClient);
        inAppChatInputView.addInputTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                inputCheckerHandler.removeCallbacks(inputFinishChecker);
                inAppChatInputView.setSendButtonEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
                inputFinishChecker.setInputValue(s.toString());
                inputCheckerHandler.postDelayed(inputFinishChecker, USER_INPUT_CHECKER_DELAY_MS);
            }
        });
        inAppChatInputView.setInputFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus)
                inAppChatInputView.hideKeyboard();
        });
    }

    private void initSendButton() {
        inAppChatInputView.setSendButtonClickListener(v -> {
            inAppChatInputView.getInputText();
            String text = inAppChatInputView.getInputText();
            if (StringUtils.isNotBlank(text)) {
                inAppChatClient.sendChatMessage(CommonUtils.escapeJsonString(text));
                inAppChatInputView.clearInputText();
            }
        });
    }

    private void initAttachmentButton() {
        inAppChatInputView.setAttachmentButtonClickListener(v -> {
            chooseFile();
        });
    }

    private void applyInAppChatInputStyle() {
        if (widgetInfo != null) {
            inAppChatInputView.applyWidgetInfoStyle(widgetInfo);
        }
    }

    //region Attachment
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
    //endregion
    //endregion

    @SuppressLint({"AddJavascriptInterface", "SetJavaScriptEnabled"})
    private void initWebView() {
        webView = containerView.findViewById(R.id.ib_lc_wv_in_app_chat);
        webView.setup(this);
        inAppChatClient = new InAppChatClientImpl(webView);
    }

    private void loadWebPage(Boolean force) {
        if (webView == null) return;
        InAppChat.JwtProvider jwtProvider = InAppChat.getInstance(requireContext()).getJwtProvider();
        String jwt = jwtProvider != null ? jwtProvider.provideJwt() : null;
        webView.loadWebPage(force, widgetInfo, jwt);
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
        applyLanguage();
    }

    private void applyLanguage() {
        String storedLanguage = widgetInfo.getLanguage();
        String language;
        if (StringUtils.isNotBlank(storedLanguage)) {
            language = storedLanguage;
        } else {
            language = mobileMessagingCore.getInstallation().getLanguage();
        }
        Locale locale = localizationUtils.localeFromString(language);
        setLanguage(locale);
    }

    public void setLanguage(Locale locale) {
        MobileMessagingLogger.d("InAppChat", "setLanguage(" + locale.toString() + ")");
        inAppChatClient.setLanguage(locale.getLanguage()); //LC widget uses only language
        localizationUtils.setLanguage(locale); //native parts use language and country code
        localisation();
    }

    private void localisation() {
        LocalizationUtils localization = LocalizationUtils.getInstance(requireContext());
        inAppChatInputView.refreshLocalisation(localizationUtils);
        noConnectionErrorToast.setText(localization.getString(R.string.ib_chat_no_connection));
    }

    @Override
    public void setControlsEnabled(boolean isEnabled) {
        inAppChatInputView.setEnabled(isEnabled);
        isWebViewLoaded = isEnabled;
        if (isEnabled) InAppChatImpl.getInstance(getContext()).resetMessageCounter();
    }

    @Override
    public void onJSError(String message) {
        chatErrors().insertError(new InAppChatErrors.Error(InAppChatErrors.JS_ERROR, message));
        webView.setVisibility(View.GONE);
        spinner.setVisibility(View.GONE);
        setControlsEnabled(false);
    }

    @Override
    public void setControlsVisibility(boolean isVisible) {
        setInputVisibility(isVisible);
    }

    private void setInputVisibility(boolean isVisible) {
        boolean canShowInMultiThread = isMultiThread() && (this.currentWidgetView == InAppChatWidgetView.THREAD || this.currentWidgetView == InAppChatWidgetView.SINGLE_MODE_THREAD);
        boolean isNotMultiThread = !isMultiThread();
        boolean isVisibleMultiThreadSafe = isVisible && (canShowInMultiThread || isNotMultiThread);

        if (isInputControlsVisible == isVisibleMultiThreadSafe) {
            return;
        } else if (isVisibleMultiThreadSafe) {
            inAppChatInputView.show(true);
        } else {
            inAppChatInputView.show(false);
        }
        isInputControlsVisible = isVisibleMultiThreadSafe;
    }

    @Override
    public void openAttachmentPreview(String url, String type, String caption) {
        fragmentCouldBePaused = false;
        Intent intent = InAppChatAttachmentPreviewActivity.startIntent(requireContext(), url, type, caption);
        startActivity(intent);
    }

    @Override
    public void onWidgetViewChanged(InAppChatWidgetView widgetView) {
        this.currentWidgetView = widgetView;
        updateViewsVisibilityByMultiThreadView();
    }

    private void updateViewsVisibilityByMultiThreadView() {
        if (isMultiThread()) {
            if (currentWidgetView != null) {
                switch (currentWidgetView) {
                    case THREAD:
                    case SINGLE_MODE_THREAD:
                        setInputVisibility(true);
                        break;
                    case LOADING:
                    case THREAD_LIST:
                    case CLOSED_THREAD:
                    case LOADING_THREAD:
                        setInputVisibility(false);
                        break;
                }
            }
        } else {
            setInputVisibility(true);
        }
    }

    public void sendContextualMetaData(String data, InAppChatMultiThreadFlag multiThreadFlag) {
        inAppChatClient.sendContextualData(data, multiThreadFlag);
    }

    private void initBackPressHandler() {
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);
    }

    private void removeBackPressHandler() {
        backPressedCallback.remove();
    }

    private boolean isMultiThread() {
        return widgetInfo != null && widgetInfo.isMultiThread();
    }

    //region Errors handling
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
                    chatErrors().insertError(new InAppChatErrors.Error(InAppChatErrors.INTERNET_CONNECTION_ERROR, getString(R.string.ib_chat_no_connection)));
                } else {
                    chatErrors().removeError(InAppChatErrors.INTERNET_CONNECTION_ERROR);
                }
            } else if (action.equals(InAppChatEvent.CHAT_CONFIGURATION_SYNCED.getKey())) {
                if (!chatErrors().removeError(InAppChatErrors.CONFIG_SYNC_ERROR)) {
                    updateViews();
                    loadWebPage(true);
                }
            } else if (action.equals(Event.API_COMMUNICATION_ERROR.getKey()) && intent.hasExtra(BroadcastParameter.EXTRA_EXCEPTION)) {
                MobileMessagingError mobileMessagingError = (MobileMessagingError) intent.getSerializableExtra(BroadcastParameter.EXTRA_EXCEPTION);
                String errorCode = mobileMessagingError.getCode();
                if (errorCode.equals(CHAT_SERVICE_ERROR) || errorCode.equals(CHAT_WIDGET_NOT_FOUND)) {
                    chatErrors().insertError(new InAppChatErrors.Error(InAppChatErrors.CONFIG_SYNC_ERROR, mobileMessagingError.getMessage()));
                }
            } else if (action.equals(Event.REGISTRATION_CREATED.getKey())) {
                syncInAppChatConfigIfNeeded();
            }
        }
    };

    private InAppChatErrors chatErrors = null;

    private InAppChatErrors chatErrors() {
        if (chatErrors == null) {
            chatErrors = new InAppChatErrors((newErrors, removedError, insertedError) -> {

                if (removedError != null) {
                    //reload webView if it wasn't loaded in case when internet connection appeared
                    if (InAppChatErrors.INTERNET_CONNECTION_ERROR.equals(removedError.getType()) && !isWebViewLoaded) {
                        loadWebPage(true);
                    }

                    //update views configuration and reload webPage in case there was config sync error
                    if (InAppChatErrors.CONFIG_SYNC_ERROR.equals(removedError.getType())) {
                        updateViews();
                        loadWebPage(true);
                    }
                }

                if (newErrors.isEmpty()) {
                    hideNoInternetConnectionView(CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS);
                } else {
                    for (InAppChatErrors.Error error : newErrors) {
                        if (InAppChatErrors.INTERNET_CONNECTION_ERROR.equals(error.getType())) {
                            showNoInternetConnectionView(CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS);
                        } else if (InAppChatErrors.CONFIG_SYNC_ERROR.equals(error.getType()) || InAppChatErrors.JS_ERROR.equals(error.getType())) {
                            showError(error.getMessage());
                        } else {
                            MobileMessagingLogger.e("InAppChat", "Unhandled error " + error);
                        }
                    }
                }
            });
        }
        return chatErrors;
    }

    private void showNoInternetConnectionView(int duration) {
        if (!chatNotAvailableViewShown) {
            noConnectionErrorToast.setVisibility(View.VISIBLE);
            noConnectionErrorToast.animate().translationY(chatNotAvailableViewHeight).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    noConnectionErrorToast.setVisibility(View.VISIBLE);
                }
            });
        }
        chatNotAvailableViewShown = true;
    }

    private void hideNoInternetConnectionView(int duration) {
        if (chatNotAvailableViewShown) {
            noConnectionErrorToast.animate().translationY(0).setDuration(duration).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    noConnectionErrorToast.setVisibility(View.INVISIBLE);
                }
            });
        }
        chatNotAvailableViewShown = false;
    }
    //endregion

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

    //region PermissionsRequester
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
    //endregion

    //region Helper methods to get FragmentActivity properties
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
        return requireContext().getTheme();
    }
    //endregion

    /**
     * Function allows another way how to inject InAppChatActionBarProvider to InAppChatFragment.
     */
    public void setInAppChatActionBarProvider(InAppChatActionBarProvider inAppChatActionBarProvider) {
        //it is used in React Native plugin to handle multithread navigation
        this.inAppChatActionBarProvider = inAppChatActionBarProvider;
    }

    @Nullable
    private InAppChatActionBarProvider getInAppChatActionBarProvider() {
        if (this.inAppChatActionBarProvider != null)
            return this.inAppChatActionBarProvider;
        else if (getFragmentActivity() instanceof InAppChatActionBarProvider)
            return (InAppChatActionBarProvider) getFragmentActivity();
        else
            return null;
    }

    private void showError(String message) {
        Snackbar.make(mainWindow, getString(R.string.ib_chat_error, message), Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ib_chat_ok, v -> {
                })
                .show();
    }
}