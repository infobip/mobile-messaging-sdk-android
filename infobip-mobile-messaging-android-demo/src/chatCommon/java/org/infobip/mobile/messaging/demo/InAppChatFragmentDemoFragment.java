package org.infobip.mobile.messaging.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.chat.core.InAppChatException;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetMessage;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetResult;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThread;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThreads;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView;
import org.infobip.mobile.messaging.chat.models.MessagePayload;
import org.infobip.mobile.messaging.chat.view.InAppChatFragment;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.StringUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import kotlin.Unit;

public class InAppChatFragmentDemoFragment extends Fragment {

    static final String ARG_WITH_TOOLBAR = "InAppChatFragmentDemoFragment.ARG_WITH_TOOLBAR";
    static final String ARG_WITH_INPUT = "InAppChatFragmentDemoFragment.ARG_WITH_INPUT";
    private static final String TAG = "InAppChatFragmentDemo";

    private View rootView;
    private FragmentContainerView inAppChatFragmentContainer;
    private Button navigateBackButton, sendButton;
    private Group inputGroup;
    private CheckBox toolbarCheckBox, inputCheckBox;
    private TextInputEditText messageInput;
    /**
     * Set to true if you want to use InAppChatFragment's ActionBar
     */
    private boolean withInAppChatToolbar = true;
    /**
     * Set to true if you want to use InAppChatFragment's MessageInput
     */
    private boolean withInAppChatInput = false;
    private InAppChatFragment fragment;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_in_app_chat_fragment_demo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        inAppChatFragmentContainer = rootView.findViewById(R.id.chatFragmentContainer);
        navigateBackButton = rootView.findViewById(R.id.navigateBack);
        sendButton = rootView.findViewById(R.id.send);
        messageInput = rootView.findViewById(R.id.messageInput);
        inputGroup = rootView.findViewById(R.id.chatInputGroup);
        toolbarCheckBox = rootView.findViewById(R.id.toolbarCheckbox);
        inputCheckBox = rootView.findViewById(R.id.inputCheckbox);

        Bundle args = getArguments();
        if (args != null) {
            withInAppChatToolbar = args.getBoolean(ARG_WITH_TOOLBAR, withInAppChatToolbar);
            withInAppChatInput = args.getBoolean(ARG_WITH_INPUT, withInAppChatInput);
        }

        setUpInAppChatFragment(withInAppChatToolbar, withInAppChatInput);
        updateCustomToolbar(withInAppChatToolbar); //Use your custom ActionBar/navigation UI if you don't use InAppChatFragment's ActionBar
        updateCustomMessageInput(withInAppChatInput); //Use your custom UI to compose message if you don't use InAppChatFragment's MessageInput
        setUpCheckboxes();
    }

    //region InAppChatFragment
    private InAppChatFragment getInAppChatFragment() {
        if (fragment == null) {
            fragment = new InAppChatFragment();
        }
        return fragment;
    }

    private InAppChatFragment.EventsListener createEventsListener(boolean withInAppChatToolbar) {
        return new InAppChatFragment.EventsListener() {

            @Override
            public void onChatLanguageChanged(@NonNull LivechatWidgetResult<String> result) {
                MobileMessagingLogger.d(TAG, "On chat language changed: " + result);
            }

            @Override
            public void onChatThreadCreated(@NonNull LivechatWidgetResult<? extends LivechatWidgetMessage> result) {
                MobileMessagingLogger.d(TAG, "On chat thread created: " + result);
            }

            @Override
            public void onChatThreadListShown(@NonNull LivechatWidgetResult<Unit> result) {
                MobileMessagingLogger.d(TAG, "On chat thread list shown: " + result);
            }

            @Override
            public void onChatThreadShown(@NonNull LivechatWidgetResult<LivechatWidgetThread> result) {
                MobileMessagingLogger.d(TAG, "On chat thread shown: " + result);
            }

            @Override
            public void onChatActiveThreadReceived(@NonNull LivechatWidgetResult<LivechatWidgetThread> result) {
                MobileMessagingLogger.d(TAG, "On chat active thread received: " + result);
            }

            @Override
            public void onChatThreadsReceived(@NonNull LivechatWidgetResult<LivechatWidgetThreads> result) {
                MobileMessagingLogger.d(TAG, "On chat threads received: " + result);
            }

            @Override
            public void onChatContextualDataSent(@NonNull LivechatWidgetResult<String> result) {
                MobileMessagingLogger.d(TAG, "On chat contextual data sent: " + result);
            }

            @Override
            public void onChatSent(@NonNull LivechatWidgetResult<? extends LivechatWidgetMessage> result) {
                MobileMessagingLogger.d(TAG, "On chat sent: " + result);
            }

            @Override
            public void onChatLoadingFinished(@NonNull LivechatWidgetResult<Unit> result) {
                MobileMessagingLogger.d(TAG, "On chat loading finished: " + result);
            }

            @Override
            public void onChatRawMessageReceived(@NonNull String rawMessage) {
                MobileMessagingLogger.d(TAG, "On chat raw message received: " + rawMessage);
            }

            @Override
            public void onChatWidgetThemeChanged(@NonNull LivechatWidgetResult<String> result) {
                MobileMessagingLogger.d(TAG, "On chat widget theme changed: " + result);
            }

            @Override
            public void onChatWidgetInfoUpdated(@NonNull WidgetInfo widgetInfo) {
                MobileMessagingLogger.d(TAG, "On chat widget info updated: " + widgetInfo);
            }

            @Override
            public void onChatViewChanged(@NonNull LivechatWidgetView widgetView) {
                MobileMessagingLogger.d(TAG, "On chat view changed: " + widgetView);
                switch (widgetView) {
                    case LOADING:
                    case THREAD_LIST:
                    case LOADING_THREAD:
                    case CLOSED_THREAD:
                        sendButton.setEnabled(false);
                        break;
                    case SINGLE_MODE_THREAD:
                    case THREAD:
                        sendButton.setEnabled(true);
                        break;
                }
            }

            @Override
            public void onChatControlsVisibilityChanged(boolean isVisible) {
                MobileMessagingLogger.d(TAG, "On chat controls visibility changed: " + isVisible);
            }

            @Override
            public void onChatConnectionResumed(@NonNull LivechatWidgetResult<Unit> result) {
                MobileMessagingLogger.d(TAG, "On chat connection resumed: " + result);
            }

            @Override
            public void onChatConnectionPaused(@NonNull LivechatWidgetResult<Unit> result) {
                MobileMessagingLogger.d(TAG, "On chat connection paused: " + result);
            }

            @Override
            public boolean onChatAttachmentPreviewOpened(@Nullable String url, @Nullable String type, @Nullable String caption) {
                MobileMessagingLogger.d(TAG, "On chat attachment preview opened: url=" + url + ", type=" + type + ", caption=" + caption);
                return false;
            }

            @Override
            public void onExitChatPressed() {
                MobileMessagingLogger.d(TAG, "On exit chat pressed");
                if (withInAppChatToolbar) {
                    showActionBar();
                }
                FragmentTransaction fragmentTransaction = InAppChatFragmentDemoFragment.this.getParentFragmentManager().beginTransaction();
                fragmentTransaction.remove(InAppChatFragmentDemoFragment.this);
                fragmentTransaction.commit();
            }
        };
    }

    private void updateInAppChatFragment(
            boolean withInAppChatToolbar,
            boolean withInAppChatInput
    ) {
        InAppChatFragment fragment = getInAppChatFragment();
        fragment.setEventsListener(createEventsListener(withInAppChatToolbar));
        if (fragment.getWithToolbar() != withInAppChatToolbar) {
            fragment.setWithToolbar(withInAppChatToolbar);
        }
        if (fragment.getWithInput() != withInAppChatInput) {
            fragment.setWithInput(withInAppChatInput);
        }
    }

    private void setUpInAppChatFragment(
            boolean withInAppChatToolbar,
            boolean withInAppChatInput
    ) {
        InAppChatFragment fragment = getInAppChatFragment();
        updateInAppChatFragment(withInAppChatToolbar, withInAppChatInput);
        fragment.setErrorsHandler(new InAppChatFragment.ErrorsHandler() {

            @Override
            public boolean handleError(@NonNull InAppChatException exception) {
                //Return true if you handled the exception, otherwise fragment's default handler will be used
                return fragment.getDefaultErrorsHandler().handleError(exception);
            }

            @Override
            public void handlerError(@NonNull String error) {
                //Your custom handling of general error or use default handler
                fragment.getDefaultErrorsHandler().handlerError(error);
            }

            @Override
            public void handlerWidgetError(@NonNull String error) {
                //Your custom handling of Livechat widget error or use default handler
                fragment.getDefaultErrorsHandler().handlerWidgetError(error);
            }

            @Override
            public void handlerNoInternetConnectionError(boolean hasConnection) {
                //Your custom handling of missing network connection error or use default handler
                fragment.getDefaultErrorsHandler().handlerNoInternetConnectionError(hasConnection);
            }

        });
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(inAppChatFragmentContainer.getId(), fragment);
        fragmentTransaction.commit();
        fm.executePendingTransactions();
    }
    //endregion

    //region Custom Toolbar
    private void updateCustomToolbar(boolean withInAppChatToolbar) {
        navigateBackButton.setVisibility(withInAppChatToolbar ? View.GONE : View.VISIBLE);
        if (withInAppChatToolbar) {
            hideActionBar();
        }
        else {
            setUpNavigateBackButton();
        }
    }

    private void setUpNavigateBackButton() {
        navigateBackButton.setOnClickListener((v) -> getInAppChatFragment().navigateBackOrCloseChat());
    }

    private ActionBar getActivityActionBar() {
        Activity activity = requireActivity();
        if (activity instanceof AppCompatActivity) {
            return ((AppCompatActivity) activity).getSupportActionBar();
        }
        return null;
    }

    private void hideActionBar() {
        ActionBar activityActionBar = getActivityActionBar();
        if (activityActionBar != null) {
            activityActionBar.hide();
        }
    }

    private void showActionBar() {
        ActionBar activityActionBar = getActivityActionBar();
        if (activityActionBar != null) {
            activityActionBar.show();
        }
    }
    //endregion

    //region Custom MessageInput
    private void updateCustomMessageInput(boolean withInAppChatInput) {
        inputGroup.setVisibility(withInAppChatInput ? View.GONE : View.VISIBLE);
        if (!withInAppChatInput) {
            setUpMessageInput();
        }
    }

    private void setUpMessageInput() {
        sendButton.setOnClickListener(v -> {
            Editable text = messageInput.getText();
            if (text != null) {
                String message = text.toString();
                if (StringUtils.isNotBlank(message)) {
                    try {
                        getInAppChatFragment().send(new MessagePayload.Basic(message));
                    } catch (IllegalArgumentException e) {
                        String error = e.getMessage();
                        if (StringUtils.isNotBlank(error)) {
                            Snackbar.make(rootView, error, Snackbar.LENGTH_SHORT).show();
                        }
                    }
                    text.clear();
                }
            }
        });
    }
    //endregion

    private void setUpCheckboxes() {
        toolbarCheckBox.setChecked(withInAppChatToolbar);
        toolbarCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            withInAppChatToolbar = isChecked;
            updateCustomToolbar(withInAppChatToolbar);
            updateInAppChatFragment(withInAppChatToolbar, withInAppChatInput);
            Bundle arguments = getArguments();
            if (arguments != null) {
                arguments.putBoolean(ARG_WITH_TOOLBAR, withInAppChatToolbar);
            }
        });
        inputCheckBox.setChecked(withInAppChatInput);
        inputCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            withInAppChatInput = isChecked;
            updateCustomMessageInput(withInAppChatInput);
            updateInAppChatFragment(withInAppChatToolbar, withInAppChatInput);
            Bundle arguments = getArguments();
            if (arguments != null) {
                arguments.putBoolean(ARG_WITH_INPUT, withInAppChatInput);
            }
        });
    }

}
