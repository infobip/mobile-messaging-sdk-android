package org.infobip.mobile.messaging.demo;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.platform.Platform;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(this.<Toolbar>findViewById(R.id.toolbar));

        this.findViewById(R.id.test_banner_big_top).setOnClickListener(v -> {
            JSONObject object = new JSONObject();
            JSONObject inAppDetails = new JSONObject();
            try {
                inAppDetails.put("url", "file:///android_asset/banner_big_text.html");
                inAppDetails.put("position", 0);
                inAppDetails.put("type", 0);
                object.put("inAppDetails", inAppDetails);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Platform.mobileMessageHandler.get(this).handleMessage(generateNewMessage(object));
        });

        this.findViewById(R.id.test_banner_small_top).setOnClickListener(v -> {
            JSONObject object = new JSONObject();
            JSONObject inAppDetails = new JSONObject();
            try {
                inAppDetails.put("url", "file:///android_asset/banner_small_text.html");
                inAppDetails.put("position", 0);
                inAppDetails.put("type", 0);
                object.put("inAppDetails", inAppDetails);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Platform.mobileMessageHandler.get(this).handleMessage(generateNewMessage(object));
        });

        this.findViewById(R.id.test_banner_big_bottom).setOnClickListener(v -> {
            JSONObject object = new JSONObject();
            JSONObject inAppDetails = new JSONObject();
            try {
                inAppDetails.put("url", "file:///android_asset/banner_big_text.html");
                inAppDetails.put("position", 1);
                inAppDetails.put("type", 0);
                object.put("inAppDetails", inAppDetails);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Platform.mobileMessageHandler.get(this).handleMessage(generateNewMessage(object));
        });

        this.findViewById(R.id.test_banner_small_bottom).setOnClickListener(v -> {
            JSONObject object = new JSONObject();
            JSONObject inAppDetails = new JSONObject();
            try {
                inAppDetails.put("url", "file:///android_asset/banner_small_text.html");
                inAppDetails.put("position", 1);
                inAppDetails.put("type", 0);
                object.put("inAppDetails", inAppDetails);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Platform.mobileMessageHandler.get(this).handleMessage(generateNewMessage(object));
        });

        this.findViewById(R.id.test_banner_popup).setOnClickListener(v -> {
            JSONObject object = new JSONObject();
            JSONObject inAppDetails = new JSONObject();
            try {
                inAppDetails.put("url", "file:///android_asset/static.html");
                inAppDetails.put("position", 0);
                inAppDetails.put("type", 1);
                object.put("inAppDetails", inAppDetails);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Platform.mobileMessageHandler.get(this).handleMessage(generateNewMessage(object));
        });

        this.findViewById(R.id.test_banner_fullscreen).setOnClickListener(v -> {
            JSONObject object = new JSONObject();
            JSONObject inAppDetails = new JSONObject();
            try {
                inAppDetails.put("url", "file:///android_asset/static_fullscreen.html");
                inAppDetails.put("position", 0);
                inAppDetails.put("type", 2);
                object.put("inAppDetails", inAppDetails);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Platform.mobileMessageHandler.get(this).handleMessage(generateNewMessage(object));
        });

        this.findViewById(R.id.test_native_inapp).setOnClickListener(v -> {
            Message nativeMessage = new Message();
            nativeMessage.setBody("some text");
            nativeMessage.setContentUrl("some text");
            nativeMessage.setMessageId(UUID.randomUUID().toString());
            nativeMessage.setInAppStyle(Message.InAppStyle.MODAL);
            Platform.mobileMessageHandler.get(this).handleMessage(nativeMessage);
        });

    }

    @NonNull
    private Message generateNewMessage(JSONObject object) {
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setBody("some text");
        message.setContentUrl("some text");
        message.setInternalData(object.toString());
        return message;
    }
}
