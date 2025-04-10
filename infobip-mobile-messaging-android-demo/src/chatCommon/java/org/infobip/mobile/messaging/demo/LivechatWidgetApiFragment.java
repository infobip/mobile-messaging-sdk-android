package org.infobip.mobile.messaging.demo;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import org.infobip.mobile.messaging.chat.InAppChat;
import org.infobip.mobile.messaging.chat.core.MultithreadStrategy;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApi;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetEventsListener;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetResult;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThread;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThreads;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView;

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
    private static final String TAG = "LivechatWidgetApiFragment";
    private LivechatWidgetApi livechatWidgetApi = null;
    private Flow buttonsFlow;
    private ConstraintLayout constraintLayout;

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
        buttonsFlow = view.findViewById(R.id.buttonsFlow);
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
        setLivechatWidgetEventsListener();
        addButtons();
    }

    private void addLog(String label, String message) {
        SpannableString spannable = new SpannableString(label);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        logTextView.append(spannable);
        logTextView.append(message + "\n\n");
    }

    private void removeFragment() {
        FragmentTransaction fragmentTransaction = getParentFragmentManager().beginTransaction();
        fragmentTransaction.remove(LivechatWidgetApiFragment.this);
        fragmentTransaction.commit();
    }

    private void setLivechatWidgetEventsListener() {
        livechatWidgetApi.setEventsListener(new LivechatWidgetEventsListener() {
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
            public void onMessageSent(@NonNull LivechatWidgetResult<String> result) {
                addLog("onMessageSent result: ", result.toString());
            }

            @Override
            public void onDraftSent(@NonNull LivechatWidgetResult<String> result) {
                addLog("onDraftSent result: ", result.toString());
            }

            @Override
            public void onContextualDataSent(@NonNull LivechatWidgetResult<String> result) {
                addLog("onContextualDataSent result: ", result.toString());
            }

            @Override
            public void onThreadsReceived(@NonNull LivechatWidgetResult<LivechatWidgetThreads> result) {
                addLog("onThreadsReceived result: ", result.toString());
            }

            @Override
            public void onActiveThreadReceived(@NonNull LivechatWidgetResult<LivechatWidgetThread> result) {
                addLog("onActiveThreadReceived result: ", result.toString());
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

        MaterialButton sendMessageButton = new MaterialButton(requireContext());
        sendMessageButton.setId(View.generateViewId());
        sendMessageButton.setText("Send chat message");
        sendMessageButton.setOnClickListener(v -> livechatWidgetApi.sendMessage("Demo message"));
        addButtonToFlow(sendMessageButton);

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

}
