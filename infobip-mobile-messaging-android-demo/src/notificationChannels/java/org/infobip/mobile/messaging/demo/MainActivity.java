/*
 * MainActivity.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.notification.NotificationChannelConfig;
import org.infobip.mobile.messaging.notification.NotificationChannelManager;
import org.infobip.mobile.messaging.notification.NotificationChannelRouter;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Selects the SDK notification channel scenario, posts simulated messages through the SDK notification handler
 * and shows which channels exist and which channel each notification was posted to.
 */
public class MainActivity extends AppCompatActivity {

    private static final long DELAYED_POST_MILLIS = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private NotificationChannelScenario activeScenario;
    private RadioGroup scenarioGroup;
    private CheckBox delayedCheckBox;
    private TextView channelsTextView;
    private TextView logTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(this.<Toolbar>findViewById(R.id.toolbar));

        activeScenario = NotificationChannelScenario.load(this);
        scenarioGroup = findViewById(R.id.rg_scenarios);
        delayedCheckBox = findViewById(R.id.cb_delayed);
        channelsTextView = findViewById(R.id.tv_channels);
        logTextView = findViewById(R.id.tv_log);

        this.<TextView>findViewById(R.id.tv_active_scenario).setText(getString(R.string.active_scenario, activeScenario.title, activeScenario.description));
        this.<TextView>findViewById(R.id.tv_config).setText(persistedConfig());

        setUpScenarios();
        setUpMessages();
        setUpActions();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            log("Android " + Build.VERSION.RELEASE + " has no notification channels, only legacy behaviour can be checked");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshChannels();
        this.<TextView>findViewById(R.id.tv_registration_id).setText(getString(R.string.registration_id, registrationId()));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // region scenarios

    private void setUpScenarios() {
        for (NotificationChannelScenario scenario : NotificationChannelScenario.values()) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setId(View.generateViewId());
            radioButton.setText(scenario.title);
            radioButton.setTag(scenario);
            scenarioGroup.addView(radioButton);
            if (scenario == activeScenario) {
                scenarioGroup.check(radioButton.getId());
            }
        }
        findViewById(R.id.btn_apply_scenario).setOnClickListener(v -> applySelectedScenario());
    }

    private void applySelectedScenario() {
        View checked = scenarioGroup.findViewById(scenarioGroup.getCheckedRadioButtonId());
        if (checked == null) return;

        NotificationChannelScenario scenario = (NotificationChannelScenario) checked.getTag();
        scenario.save(this);
        Log.d(NotificationChannelsApplication.TAG, "Restarting with scenario " + scenario.name());

        // SDK is initialized in Application.onCreate, so a new process is needed to apply another setup
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launchIntent != null && launchIntent.getComponent() != null) {
            startActivity(Intent.makeRestartActivityTask(launchIntent.getComponent()));
        }
        Runtime.getRuntime().exit(0);
    }

    private String persistedConfig() {
        NotificationChannelConfig config = NotificationChannelManager.loadConfig(this);
        return getString(R.string.persisted_config, config != null ? config.toJson() : "null");
    }

    // endregion

    // region simulated messages

    private void setUpMessages() {
        LinearLayout container = findViewById(R.id.ll_messages);
        addMessageButton(container, "Normal (default sound + vibration)", () -> message("Normal", "default", true));
        addMessageButton(container, "Banner (heads-up)", () -> banner(message("Banner", "default", true)));
        addMessageButton(container, "Silent (no sound, no vibration)", () -> message("Silent", null, false));
        addMessageButton(container, "Vibration only", () -> message("Vibration only", null, true));
        addMessageButton(container, "Custom sound \"ding\"", () -> message("Custom sound", NotificationChannelScenario.SOUND_DING, true));
        addMessageButton(container, "Custom sound \"ding\" + banner", () -> banner(message("Custom sound banner", NotificationChannelScenario.SOUND_DING, true)));
        addMessageButton(container, "Chat message", () -> {
            Message message = message("Chat", "default", true);
            message.setMessageType(Message.MESSAGE_TYPE_CHAT);
            return message;
        });
        addMessageButton(container, "Payload androidChannelId=promo + banner",
                () -> banner(withPayload(message("Payload promo", "default", true), NotificationChannelScenario.PAYLOAD_CHANNEL_ID_KEY, "promo")));
        addMessageButton(container, "Payload androidChannelId=does_not_exist",
                () -> withPayload(message("Payload missing channel", "default", true), NotificationChannelScenario.PAYLOAD_CHANNEL_ID_KEY, "does_not_exist"));
        addMessageButton(container, "Payload type=order (resolver)", () -> withPayload(message("Order", "default", true), "type", "order"));
        addMessageButton(container, "Payload type=crash (resolver throws)", () -> withPayload(message("Resolver throws", "default", true), "type", "crash"));
    }

    private interface MessageFactory {
        Message create();
    }

    private void addMessageButton(LinearLayout container, String label, MessageFactory factory) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(v -> {
            Message message = factory.create();
            if (delayedCheckBox.isChecked()) {
                log("\"" + label + "\" will be posted in " + DELAYED_POST_MILLIS / 1000 + " s");
                handler.postDelayed(() -> postMessage(label, message), DELAYED_POST_MILLIS);
            } else {
                postMessage(label, message);
            }
        });
        container.addView(button, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void postMessage(String label, Message message) {
        int notificationId = MobileMessagingCore.getInstance(this).getNotificationHandler().displayNotification(message);
        if (notificationId == -1) {
            log("\"" + label + "\": notification not displayed (notifications disabled or permission missing?)");
            return;
        }

        // posting is asynchronous in the system, read the channel a bit later
        handler.postDelayed(() -> {
            log("\"" + label + "\" → " + postedChannelId(notificationId));
            refreshChannels();
        }, 300);
    }

    private String postedChannelId(int notificationId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return "posted (no channels on this Android version)";
        }
        NotificationManager notificationManager = notificationManager();
        for (StatusBarNotification notification : notificationManager.getActiveNotifications()) {
            if (notification.getId() == notificationId) {
                return notification.getNotification().getChannelId();
            }
        }
        return "NOT SHOWN (dropped by the system or blocked)";
    }

    private static Message message(String title, @Nullable String sound, boolean vibrate) {
        Message message = new Message();
        message.setTitle(title);
        message.setBody("Simulated message, sound=" + sound + ", vibrate=" + vibrate);
        message.setSound(sound);
        message.setVibrate(vibrate);
        return message;
    }

    private static Message banner(Message message) {
        message.setInAppStyle(Message.InAppStyle.BANNER);
        return message;
    }

    private static Message withPayload(Message message, String key, String value) {
        try {
            JSONObject customPayload = message.getCustomPayload() != null ? message.getCustomPayload() : new JSONObject();
            message.setCustomPayload(customPayload.put(key, value));
        } catch (JSONException e) {
            Log.e(NotificationChannelsApplication.TAG, "Unable to set custom payload", e);
        }
        return message;
    }

    // endregion

    // region actions

    private void setUpActions() {
        findViewById(R.id.btn_refresh_channels).setOnClickListener(v -> refreshChannels());
        findViewById(R.id.btn_create_orders).setOnClickListener(v -> createOrdersChannel());
        findViewById(R.id.btn_delete_orders).setOnClickListener(v -> deleteChannel(NotificationChannelScenario.ORDERS_CHANNEL_ID));
        findViewById(R.id.btn_delete_sdk_defaults).setOnClickListener(v -> {
            boolean deleted = MobileMessaging.getInstance(this).deleteSdkDefaultNotificationChannels();
            log("deleteSdkDefaultNotificationChannels() = " + deleted);
            refreshChannels();
        });
        findViewById(R.id.btn_delete_all_channels).setOnClickListener(v -> deleteAllChannels());
        findViewById(R.id.btn_channel_settings).setOnClickListener(v -> openNotificationSettings());
        findViewById(R.id.btn_cancel_notifications).setOnClickListener(v -> {
            MobileMessagingCore.getInstance(this).getNotificationHandler().cancelAllNotifications();
            log("All notifications cancelled");
        });
        findViewById(R.id.btn_copy_registration_id).setOnClickListener(v -> copyRegistrationId());
        findViewById(R.id.btn_clear_log).setOnClickListener(v -> logTextView.setText(""));
    }

    private void createOrdersChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        notificationManager().createNotificationChannel(
                new NotificationChannel(NotificationChannelScenario.ORDERS_CHANNEL_ID, "Orders", NotificationManager.IMPORTANCE_DEFAULT));
        log("App created channel \"" + NotificationChannelScenario.ORDERS_CHANNEL_ID + "\"");
        refreshChannels();
    }

    private void deleteChannel(String channelId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        notificationManager().deleteNotificationChannel(channelId);
        log("Deleted channel \"" + channelId + "\"");
        refreshChannels();
    }

    private void deleteAllChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager notificationManager = notificationManager();
        for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
            notificationManager.deleteNotificationChannel(channel.getId());
        }
        log("Deleted all channels, apply a scenario to let the SDK create them again");
        refreshChannels();
    }

    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.fromParts("package", getPackageName(), null));
        }
        startActivity(intent);
    }

    private void copyRegistrationId() {
        String registrationId = registrationId();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && registrationId != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Registration ID", registrationId));
            Toast.makeText(this, R.string.registration_id_copied, Toast.LENGTH_SHORT).show();
        }
        log("Registration ID: " + registrationId);
    }

    @Nullable
    private String registrationId() {
        return MobileMessaging.getInstance(this).getInstallation().getPushRegistrationId();
    }

    // endregion

    // region channels

    private void refreshChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            channelsTextView.setText(R.string.no_channels_support);
            return;
        }
        channelsTextView.setText(describeChannels());
    }

    @TargetApi(Build.VERSION_CODES.O)
    private String describeChannels() {
        List<NotificationChannel> channels = new ArrayList<>(notificationManager().getNotificationChannels());
        if (channels.isEmpty()) {
            return getString(R.string.no_channels);
        }
        Collections.sort(channels, (first, second) -> first.getId().compareTo(second.getId()));

        List<String> sdkDefaultIds = NotificationChannelRouter.getSdkDefaultChannelIds(NotificationChannelScenario.CUSTOM_SOUND_CHANNEL_ID);
        List<String> fallbackIds = NotificationChannelRouter.getFallbackChannelIds();
        StringBuilder sb = new StringBuilder();
        for (NotificationChannel channel : channels) {
            String marker = sdkDefaultIds.contains(channel.getId()) ? "[sdk] "
                    : fallbackIds.contains(channel.getId()) ? "[fallback] " : "";
            sb.append(marker).append(channel.getId())
                    .append("\n    name=").append(channel.getName())
                    .append(", importance=").append(importance(channel.getImportance()))
                    .append(", sound=").append(sound(channel))
                    .append(", vibration=").append(channel.shouldVibrate())
                    .append('\n');
        }
        return sb.toString().trim();
    }

    @TargetApi(Build.VERSION_CODES.O)
    private static String sound(NotificationChannel channel) {
        if (channel.getSound() == null) {
            return "none";
        }
        String sound = channel.getSound().toString();
        int rawIndex = sound.indexOf("/raw/");
        return rawIndex >= 0 ? sound.substring(rawIndex + 1) : "default";
    }

    private static String importance(int importance) {
        switch (importance) {
            case NotificationManager.IMPORTANCE_NONE:
                return "NONE (blocked)";
            case NotificationManager.IMPORTANCE_MIN:
                return "MIN";
            case NotificationManager.IMPORTANCE_LOW:
                return "LOW";
            case NotificationManager.IMPORTANCE_DEFAULT:
                return "DEFAULT";
            case NotificationManager.IMPORTANCE_HIGH:
                return "HIGH";
            default:
                return String.valueOf(importance);
        }
    }

    // endregion

    private NotificationManager notificationManager() {
        return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void log(String line) {
        Log.d(NotificationChannelsApplication.TAG, line);
        logTextView.append(timeFormat.format(new Date()) + "  " + line + "\n");
    }
}
