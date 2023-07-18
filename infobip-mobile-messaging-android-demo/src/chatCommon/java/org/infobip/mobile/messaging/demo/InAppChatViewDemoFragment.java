package org.infobip.mobile.messaging.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.textfield.TextInputEditText;

import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.chat.core.InAppChatWidgetView;
import org.infobip.mobile.messaging.chat.view.InAppChatView;
import org.infobip.mobile.messaging.util.StringUtils;

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
        chatNavigateBackButton = rootView.findViewById(R.id.chatNavigateBack);
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
        inAppChatView.init(getViewLifecycleOwner().getLifecycle());
        InAppChatView.EventsListener eventsListener = new InAppChatView.EventsListener() {

            @Override
            public void onChatWidgetInfoUpdated(@NonNull WidgetInfo widgetInfo) {
                //Useful livechat widget information
            }

            @Override
            public void onChatViewChanged(@NonNull InAppChatWidgetView widgetView) {
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
                //Handle attachment preview
            }

            @Override
            public void onChatControlsVisibilityChanged(boolean isVisible) {
                //Handle chat controls visibility change, you can show/hide input based on isVisible value
            }

            @Override
            public void onChatLoaded(boolean controlsEnabled) {
                //Chat was loaded, if controlsEnabled = true there was no error
            }

            @Override
            public void onChatDisconnected() {
                //Chat was disconnected, blank page is loaded.
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
            public void handlerNoInternetConnectionError() {
                //Your custom handling of missing network connection error or use default handler
                inAppChatView.getDefaultErrorsHandler().handlerNoInternetConnectionError();
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
                    inAppChatView.sendChatMessage(message);
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
