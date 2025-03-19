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
import org.infobip.mobile.messaging.chat.core.InAppChatWidgetView;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetResult;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThread;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThreads;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView;
import org.infobip.mobile.messaging.chat.view.InAppChatView;
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
            public void onChatWidgetThemeChanged(@NonNull LivechatWidgetResult<String> result) {
                //Widget theme was applied
            }

            @Override
            public void onChatLanguageChanged(@NonNull LivechatWidgetResult<String> result) {
                //Widget language was changed
            }

            @Override
            public void onChatThreadListShown(@NonNull LivechatWidgetResult<Unit> result) {
                //Thread list was shown
            }

            @Override
            public void onChatThreadShown(@NonNull LivechatWidgetResult<LivechatWidgetThread> result) {
                //Thread was shown
            }

            @Override
            public void onChatActiveThreadReceived(@NonNull LivechatWidgetResult<LivechatWidgetThread> result) {
                //Active thread was received
            }

            @Override
            public void onChatThreadsReceived(@NonNull LivechatWidgetResult<LivechatWidgetThreads> result) {
                //Threads were received
            }

            @Override
            public void onChatContextualDataSent(@NonNull LivechatWidgetResult<String> result) {
                //Contextual data was sent
            }

            @Override
            public void onChatDraftSent(@NonNull LivechatWidgetResult<String> result) {
                //Draft was sent
            }

            @Override
            public void onChatMessageSent(@NonNull LivechatWidgetResult<String> result) {
                //Message was sent
            }

            @Override
            public void onChatConnectionResumed(@NonNull LivechatWidgetResult<Unit> result) {
                //Chat connection was resumed
            }

            @Override
            public void onChatConnectionPaused(@NonNull LivechatWidgetResult<Unit> result) {
                //Chat connection was paused
            }

            @Override
            public void onChatLoadingFinished(@NonNull LivechatWidgetResult<Unit> result) {
                //Chat was loaded, if result.isSuccess() = true there was no error
            }

            @Override
            public void onChatAttachmentPreviewOpened(@Nullable String url, @Nullable String type, @Nullable String caption) {
                //Handle attachment preview
            }

            @Override
            public void onChatRawMessageReceived(@NonNull String rawMessage) {
                //You can use raw message for further processing on your side
            }

            @Override
            public void onChatWidgetThemeChanged(@NonNull String widgetThemeName) {
                //Deprecated, use onChatWidgetThemeChanged(@NonNull LivechatWidgetResult<String> result) instead
            }

            @Override
            public void onChatWidgetInfoUpdated(@NonNull WidgetInfo widgetInfo) {
                //Useful livechat widget information
            }

            @Override
            public void onChatViewChanged(@NonNull InAppChatWidgetView widgetView) {
                //Deprecated method, use onChatViewChanged(LivechatWidgetView widgetView) instead
            }

            @Override
            public void onChatViewChanged(@NonNull LivechatWidgetView widgetView) {
                //Handle navigation in multithread livechat widget
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
            public void onAttachmentPreviewOpened(@Nullable String url, @Nullable String type, @Nullable String caption) {
                //Deprecated, use onChatAttachmentPreviewOpened(@Nullable String url, @Nullable String type, @Nullable String caption) instead
            }

            @Override
            public void onChatControlsVisibilityChanged(boolean isVisible) {
                //Handle chat controls visibility change, you can show/hide input based on isVisible value
            }

            @Override
            public void onChatLoaded(boolean controlsEnabled) {
                //Deprecated, use onChatLoadingFinished(@NonNull LivechatWidgetResult<Unit> result) instead
            }

            @Override
            public void onChatDisconnected() {
                //Deprecated, use onChatConnectionPaused(@NonNull LivechatWidgetResult<Unit> result) instead
            }

            @Override
            public void onChatReconnected() {
                //Deprecated, use onChatConnectionResumed(@NonNull LivechatWidgetResult<Unit> result) instead
            }
        };
        inAppChatView.setEventsListener(eventsListener);
        inAppChatView.setErrorsHandler(new InAppChatView.ErrorsHandler() {
            @Override
            public void handlerError(@NonNull String error) {
                //Your custom handling of general error or use default handler
                inAppChatView.getDefaultErrorsHandler().handlerError(error);
            }

            @Override
            public void handlerWidgetError(@NonNull String error) {
                //Your custom handling of Livechat widget error or use default handler
                inAppChatView.getDefaultErrorsHandler().handlerWidgetError(error);
            }

            @Override
            public void handlerNoInternetConnectionError(boolean hasConnection) {
                //Your custom handling of missing network connection error or use default handler
                inAppChatView.getDefaultErrorsHandler().handlerNoInternetConnectionError(hasConnection);
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
                        inAppChatView.sendChatMessage(message);
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
