/*
 * LivechatWidgetApiFragment.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

import org.infobip.mobile.messaging.chat.InAppChat;
import org.infobip.mobile.messaging.chat.attachments.InAppChatAttachment;
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApi;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetEventsListener;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetMessage;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetResult;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThread;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThreads;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView;
import org.infobip.mobile.messaging.chat.models.MessagePayload;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.helper.widget.Flow;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import kotlin.Unit;

public class LivechatWidgetApiFragment extends Fragment {

    private TextView logTextView;
    private ScrollView scrollView;
    private static final String TAG = "LivechatWidgetApiDemo";
    private LivechatWidgetApi livechatWidgetApi = null;
    private Flow buttonsFlow;
    private ConstraintLayout constraintLayout;
    private AutoCompleteTextView messageTypeAutoCompleteTextView, threadIdAutocompleteTextView;
    private TextInputLayout threadIdTextInputLayout;
    private int messageType = 0;
    private Set<String> availableThreads = new HashSet<>();
    private String threadId = null;
    MaterialButton openThreadButton = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lc_widget_api, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        constraintLayout = view.findViewById(R.id.constraintLayout);
        logTextView = view.findViewById(R.id.logTextView);
        scrollView = view.findViewById(R.id.logScrollView);
        buttonsFlow = view.findViewById(R.id.buttonsFlow);
        messageTypeAutoCompleteTextView = view.findViewById(R.id.messageTypeAutocompleteTextView);
        threadIdAutocompleteTextView = view.findViewById(R.id.threadAutocompleteTextView);
        threadIdTextInputLayout = view.findViewById(R.id.threadInputLayout);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            activity.setSupportActionBar(toolbar);
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                activity.getSupportActionBar().setTitle("Livechat Widget API");
            }
        }
        toolbar.setNavigationOnClickListener(v -> {
            removeFragment();
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                removeFragment();
            }
        });

        livechatWidgetApi = InAppChat.getInstance(requireContext()).getLivechatWidgetApi();
        setUpMessageTypeSpinner();
        setUpThreadIdSpinner();
        setLivechatWidgetEventsListener();
        addButtons();
    }

    private void addLog(String label, String message) {
        MobileMessagingLogger.d(TAG, label + message);
        SpannableString spannable = new SpannableString(label);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        logTextView.append(spannable);
        logTextView.append(message + "\n\n");
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void removeFragment() {
        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        fragmentTransaction.remove(LivechatWidgetApiFragment.this);
        fragmentTransaction.commit();
    }

    private void setLivechatWidgetEventsListener() {
        livechatWidgetApi.setEventsListener(new LivechatWidgetEventsListener() {

            @Override
            public void onSent(@NonNull LivechatWidgetResult<? extends LivechatWidgetMessage> result) {
                addLog("onSent result: ", result.toString());
            }

            @Override
            public void onPageStarted(@Nullable String url) {
                addLog("onPageStarted: ", url);
            }

            @Override
            public void onPageFinished(@Nullable String url) {
                addLog("onPageFinished: ", url);
            }

            @Override
            public void onLoadingFinished(@NonNull LivechatWidgetResult<Boolean> result) {
                addLog("onLoadingFinished result: ", result.toString());
            }

            @Override
            public void onConnectionPaused(@NonNull LivechatWidgetResult<Unit> result) {
                addLog("onConnectionPaused result: ", result.toString());
            }

            @Override
            public void onConnectionResumed(@NonNull LivechatWidgetResult<Unit> result) {
                addLog("onConnectionResumed result: ", result.toString());
            }

            @Override
            public void onContextualDataSent(@NonNull LivechatWidgetResult<String> result) {
                addLog("onContextualDataSent result: ", result.toString());
            }

            @Override
            public void onThreadsReceived(@NonNull LivechatWidgetResult<LivechatWidgetThreads> result) {
                addLog("onThreadsReceived result: ", result.toString());
                LivechatWidgetThreads threadsResult = result.getOrNull();
                if (threadsResult != null) {
                    availableThreads.clear();
                    List<LivechatWidgetThread> threads = threadsResult.getThreads();
                    if (threads != null && !threads.isEmpty()) {
                        for (LivechatWidgetThread thread : threads) {
                            if (StringUtils.isNotBlank(thread.getId())) {
                                availableThreads.add(thread.getId());
                            }
                        }
                        updateThreadsAdapter(null);
                    }
                }
            }

            @Override
            public void onActiveThreadReceived(@NonNull LivechatWidgetResult<LivechatWidgetThread> result) {
                addLog("onActiveThreadReceived result: ", result.toString());
                LivechatWidgetThread activeThread = result.getOrNull();
                if (activeThread != null) {
                    String threadId = activeThread.getId();
                    if (StringUtils.isNotBlank(threadId)) {
                        availableThreads.add(threadId);
                        updateThreadsAdapter(threadId);
                    }
                }
            }

            @Override
            public void onThreadCreated(@NonNull LivechatWidgetResult<? extends LivechatWidgetMessage> result) {
                addLog("onThreadCreated: ", result.toString());
            }

            @Override
            public void onThreadShown(@NonNull LivechatWidgetResult<LivechatWidgetThread> result) {
                addLog("onThreadShown result: ", result.toString());
            }

            @Override
            public void onThreadListShown(@NonNull LivechatWidgetResult<Unit> result) {
                addLog("onThreadListShown result: ", result.toString());
            }

            @Override
            public void onLanguageChanged(@NonNull LivechatWidgetResult<String> result) {
                addLog("onLanguageChanged result: ", result.toString());
            }

            @Override
            public void onThemeChanged(@NonNull LivechatWidgetResult<String> result) {
                addLog("onThemeChanged result: ", result.toString());
            }

            @Override
            public void onControlsVisibilityChanged(boolean visible) {
                addLog("onControlsVisibilityChanged: ", String.valueOf(visible));
            }

            @Override
            public void onAttachmentPreviewOpened(@Nullable String url, @Nullable String type, @Nullable String caption) {
                addLog("onAttachmentPreviewOpened: ", url + " " + type + " " + caption);
            }

            @Override
            public void onWidgetViewChanged(@NonNull LivechatWidgetView view) {
                addLog("onWidgetViewChanged: ", view.toString());
            }

            @Override
            public void onRawMessageReceived(@Nullable String message) {
                addLog("onRawMessageReceived: ", message);
            }
        });
    }

    private void addButtons() {
        MaterialButton loadWidgetButton = new MaterialButton(requireContext());
        loadWidgetButton.setId(View.generateViewId());
        loadWidgetButton.setText("Load widget");
        loadWidgetButton.setOnClickListener(v -> livechatWidgetApi.loadWidget());
        addButtonToFlow(loadWidgetButton);

        MaterialButton sendContextualDataButton = new MaterialButton(requireContext());
        sendContextualDataButton.setId(View.generateViewId());
        sendContextualDataButton.setText("Send contextual data");
        sendContextualDataButton.setOnClickListener(v -> livechatWidgetApi.sendContextualData("{data: 'Android Demo App'}", MultithreadStrategy.ALL_PLUS_NEW));
        addButtonToFlow(sendContextualDataButton);

        MaterialButton getThreadsButton = new MaterialButton(requireContext());
        getThreadsButton.setId(View.generateViewId());
        getThreadsButton.setText("Get threads");
        getThreadsButton.setOnClickListener(v -> livechatWidgetApi.getThreads());
        addButtonToFlow(getThreadsButton);

        MaterialButton getActiveThreadButton = new MaterialButton(requireContext());
        getActiveThreadButton.setId(View.generateViewId());
        getActiveThreadButton.setText("Get active thread");
        getActiveThreadButton.setOnClickListener(v -> livechatWidgetApi.getActiveThread());
        addButtonToFlow(getActiveThreadButton);

        MaterialButton createThreadButton = new MaterialButton(requireContext());
        createThreadButton.setId(View.generateViewId());
        createThreadButton.setText("Create thread");
        createThreadButton.setOnClickListener(v -> livechatWidgetApi.createThread(getMessagePayload()));
        addButtonToFlow(createThreadButton);

        openThreadButton = new MaterialButton(requireContext());
        openThreadButton.setId(View.generateViewId());
        openThreadButton.setText("Open thread");
        openThreadButton.setOnClickListener(v -> livechatWidgetApi.showThread(threadId));
        openThreadButton.setVisibility(View.GONE);
        addButtonToFlow(openThreadButton);

        MaterialButton sendButton = new MaterialButton(requireContext());
        sendButton.setId(View.generateViewId());
        sendButton.setText("Send");
        sendButton.setOnClickListener(v -> livechatWidgetApi.send(getMessagePayload(), threadId));
        addButtonToFlow(sendButton);

        MaterialButton resetButton = new MaterialButton(requireContext());
        resetButton.setId(View.generateViewId());
        resetButton.setText("Reset");
        resetButton.setOnClickListener(v -> livechatWidgetApi.reset());
        addButtonToFlow(resetButton);

        buttonsFlow.setWrapMode(Flow.WRAP_CHAIN);
        buttonsFlow.setHorizontalStyle(Flow.CHAIN_PACKED);
        buttonsFlow.requestLayout();

    }

    private void addButtonToFlow(MaterialButton button) {
        constraintLayout.addView(button);
        int[] referencedIds = buttonsFlow.getReferencedIds();
        int[] newReferencedIds = new int[referencedIds.length + 1];
        System.arraycopy(referencedIds, 0, newReferencedIds, 0, referencedIds.length);
        newReferencedIds[referencedIds.length] = button.getId();
        buttonsFlow.setReferencedIds(newReferencedIds);
    }

    private void setUpMessageTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(), R.array.message_types, android.R.layout.simple_dropdown_item_1line);
        messageTypeAutoCompleteTextView.setAdapter(adapter);
        messageTypeAutoCompleteTextView.setOnItemClickListener((adapterView, view, position, id) -> {
            messageType = position;
        });
        messageTypeAutoCompleteTextView.setText(adapter.getItem(0), false);
    }

    private void setUpThreadIdSpinner() {
        updateThreadsAdapter(null);
        threadIdAutocompleteTextView.setOnItemClickListener((adapterView, view, position, id) -> {
            threadId = adapterView.getItemAtPosition(position).toString();
        });
    }

    private void updateThreadsAdapter(String threadIdToSelect) {
        int visibility = availableThreads.isEmpty() ? View.GONE : View.VISIBLE;
        threadIdTextInputLayout.setVisibility(visibility);
        if (openThreadButton != null) {
            openThreadButton.setVisibility(visibility);
        }

        if (!availableThreads.isEmpty()) {
            ArrayAdapter<String> adapter = new ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    availableThreads.toArray()
            );
            threadIdAutocompleteTextView.setAdapter(adapter);
            if (threadIdToSelect != null) {
                threadIdAutocompleteTextView.setText(threadIdToSelect, false);
            }
        }
    }

    private MessagePayload getMessagePayload() {
        switch (messageType) {
            case 1:
                return new MessagePayload.Draft("Demo draft message " + System.currentTimeMillis());
            case 2:
                return new MessagePayload.Basic(
                        null,
                        new InAppChatAttachment(
                                "image/png",
                                "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAApgAAAKYB3X3/OAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAANCSURBVEiJtZZPbBtFFMZ/M7ubXdtdb1xSFyeilBapySVU8h8OoFaooFSqiihIVIpQBKci6KEg9Q6H9kovIHoCIVQJJCKE1ENFjnAgcaSGC6rEnxBwA04Tx43t2FnvDAfjkNibxgHxnWb2e/u992bee7tCa00YFsffekFY+nUzFtjW0LrvjRXrCDIAaPLlW0nHL0SsZtVoaF98mLrx3pdhOqLtYPHChahZcYYO7KvPFxvRl5XPp1sN3adWiD1ZAqD6XYK1b/dvE5IWryTt2udLFedwc1+9kLp+vbbpoDh+6TklxBeAi9TL0taeWpdmZzQDry0AcO+jQ12RyohqqoYoo8RDwJrU+qXkjWtfi8Xxt58BdQuwQs9qC/afLwCw8tnQbqYAPsgxE1S6F3EAIXux2oQFKm0ihMsOF71dHYx+f3NND68ghCu1YIoePPQN1pGRABkJ6Bus96CutRZMydTl+TvuiRW1m3n0eDl0vRPcEysqdXn+jsQPsrHMquGeXEaY4Yk4wxWcY5V/9scqOMOVUFthatyTy8QyqwZ+kDURKoMWxNKr2EeqVKcTNOajqKoBgOE28U4tdQl5p5bwCw7BWquaZSzAPlwjlithJtp3pTImSqQRrb2Z8PHGigD4RZuNX6JYj6wj7O4TFLbCO/Mn/m8R+h6rYSUb3ekokRY6f/YukArN979jcW+V/S8g0eT/N3VN3kTqWbQ428m9/8k0P/1aIhF36PccEl6EhOcAUCrXKZXXWS3XKd2vc/TRBG9O5ELC17MmWubD2nKhUKZa26Ba2+D3P+4/MNCFwg59oWVeYhkzgN/JDR8deKBoD7Y+ljEjGZ0sosXVTvbc6RHirr2reNy1OXd6pJsQ+gqjk8VWFYmHrwBzW/n+uMPFiRwHB2I7ih8ciHFxIkd/3Omk5tCDV1t+2nNu5sxxpDFNx+huNhVT3/zMDz8usXC3ddaHBj1GHj/As08fwTS7Kt1HBTmyN29vdwAw+/wbwLVOJ3uAD1wi/dUH7Qei66PfyuRj4Ik9is+hglfbkbfR3cnZm7chlUWLdwmprtCohX4HUtlOcQjLYCu+fzGJH2QRKvP3UNz8bWk1qMxjGTOMThZ3kvgLI5AzFfo379UAAAAASUVORK5CYII=",
                                "DemoAttachment" + System.currentTimeMillis() + ".png"
                        )
                );
            case 3:
                return new MessagePayload.CustomData(
                        "{" +
                                "  \"name\": \"scratch_97\"," +
                                "  \"description\": \"This is a custom \\\"data\\\" file 'for' scratch_97.\"," +
                                "  \"version\": \"1.0\"," +
                                "  \"author\": \"Livechat user\"" +
                                "}",
                        "agentMessage",
                        "userMessage"
                );
            case 4:
                return new MessagePayload.Basic(
                        "Demo message with \"double quotes\" and 'single quotes' " + System.currentTimeMillis(),
                        new InAppChatAttachment(
                                "image/png",
                                "iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAApgAAAKYB3X3/OAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAANCSURBVEiJtZZPbBtFFMZ/M7ubXdtdb1xSFyeilBapySVU8h8OoFaooFSqiihIVIpQBKci6KEg9Q6H9kovIHoCIVQJJCKE1ENFjnAgcaSGC6rEnxBwA04Tx43t2FnvDAfjkNibxgHxnWb2e/u992bee7tCa00YFsffekFY+nUzFtjW0LrvjRXrCDIAaPLlW0nHL0SsZtVoaF98mLrx3pdhOqLtYPHChahZcYYO7KvPFxvRl5XPp1sN3adWiD1ZAqD6XYK1b/dvE5IWryTt2udLFedwc1+9kLp+vbbpoDh+6TklxBeAi9TL0taeWpdmZzQDry0AcO+jQ12RyohqqoYoo8RDwJrU+qXkjWtfi8Xxt58BdQuwQs9qC/afLwCw8tnQbqYAPsgxE1S6F3EAIXux2oQFKm0ihMsOF71dHYx+f3NND68ghCu1YIoePPQN1pGRABkJ6Bus96CutRZMydTl+TvuiRW1m3n0eDl0vRPcEysqdXn+jsQPsrHMquGeXEaY4Yk4wxWcY5V/9scqOMOVUFthatyTy8QyqwZ+kDURKoMWxNKr2EeqVKcTNOajqKoBgOE28U4tdQl5p5bwCw7BWquaZSzAPlwjlithJtp3pTImSqQRrb2Z8PHGigD4RZuNX6JYj6wj7O4TFLbCO/Mn/m8R+h6rYSUb3ekokRY6f/YukArN979jcW+V/S8g0eT/N3VN3kTqWbQ428m9/8k0P/1aIhF36PccEl6EhOcAUCrXKZXXWS3XKd2vc/TRBG9O5ELC17MmWubD2nKhUKZa26Ba2+D3P+4/MNCFwg59oWVeYhkzgN/JDR8deKBoD7Y+ljEjGZ0sosXVTvbc6RHirr2reNy1OXd6pJsQ+gqjk8VWFYmHrwBzW/n+uMPFiRwHB2I7ih8ciHFxIkd/3Omk5tCDV1t+2nNu5sxxpDFNx+huNhVT3/zMDz8usXC3ddaHBj1GHj/As08fwTS7Kt1HBTmyN29vdwAw+/wbwLVOJ3uAD1wi/dUH7Qei66PfyuRj4Ik9is+hglfbkbfR3cnZm7chlUWLdwmprtCohX4HUtlOcQjLYCu+fzGJH2QRKvP3UNz8bWk1qMxjGTOMThZ3kvgLI5AzFfo379UAAAAASUVORK5CYII=",
                                "DemoAttachment" + System.currentTimeMillis() + ".png"
                        )
                );
            default:
                return new MessagePayload.Basic("Demo message with \"double quotes\" and 'single quotes' " + System.currentTimeMillis());
        }
    }

}
