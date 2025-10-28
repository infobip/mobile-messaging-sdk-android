package org.infobip.mobile.messaging.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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
import org.infobip.mobile.messaging.chat.view.InAppChatView;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.StringUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import kotlin.Unit;

public class InAppChatViewDemoFragment extends Fragment {

    private View rootView;
    private InAppChatView inAppChatView;
    private Button chatNavigateBackButton;
    private Button sendButton;
    private static final String TAG = "InAppChatViewDemo";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_in_app_chat_view_demo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootView = view;
        inAppChatView = view.findViewById(R.id.inAppChatView);
        chatNavigateBackButton = rootView.findViewById(R.id.navigateBack);
        sendButton = rootView.findViewById(R.id.send);

        hideActionBar();
        setUpExitButton();
        setUpChatNavigateBackButton();
        setUpInAppChatView();
        setUpMessageInput();
    }

    private void setUpExitButton() {
        Button exitButton = rootView.findViewById(R.id.exit);
        exitButton.setOnClickListener((v) -> {
            showActionBar();
            FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
            fragmentTransaction.remove(this);
            fragmentTransaction.commit();
        });
    }

    private void setUpChatNavigateBackButton() {
        chatNavigateBackButton.setOnClickListener((v) -> {
            if (inAppChatView.isMultiThread()) {
                inAppChatView.showThreadList();
            }
        });
    }

    private void setUpInAppChatView() {
        inAppChatView.setForceDarkAllowed(false);
        inAppChatView.init(getViewLifecycleOwner().getLifecycle());
        InAppChatView.EventsListener eventsListener = new InAppChatView.EventsListener() {

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
                    case SINGLE_MODE_THREAD:
                        chatNavigateBackButton.setEnabled(false);
                        break;
                    case LOADING_THREAD:
                    case THREAD:
                    case CLOSED_THREAD:
                        chatNavigateBackButton.setEnabled(true);
                        break;
                }
                //Handle message input multithread livechat widget
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
            public void onChatAttachmentPreviewOpened(@Nullable String url, @Nullable String type, @Nullable String caption) {
                MobileMessagingLogger.d(TAG, "On chat attachment preview opened: url=" + url + ", type=" + type + ", caption=" + caption);
            }
        };
        inAppChatView.setEventsListener(eventsListener);
        inAppChatView.setErrorsHandler(new InAppChatView.ErrorsHandler() {
            @Override
            public boolean handleError(@NonNull InAppChatException exception) {
                //Return true if you handled the exception, otherwise view's default handler will be used
                return inAppChatView.getDefaultErrorsHandler().handleError(exception);
            }
        });
    }

    private void setUpMessageInput() {
        TextInputEditText messageInput = rootView.findViewById(R.id.messageInput);
        sendButton.setOnClickListener(v -> {
            Editable text = messageInput.getText();
            if (text != null) {
                String message = text.toString();
                if (StringUtils.isNotBlank(message)) {
                    try {
                        inAppChatView.send(new MessagePayload.Basic(message));
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
}
