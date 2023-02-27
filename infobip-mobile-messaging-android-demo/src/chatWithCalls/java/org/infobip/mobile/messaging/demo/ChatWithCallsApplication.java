package org.infobip.mobile.messaging.demo;

import android.widget.Toast;

import com.infobip.webrtc.ui.InfobipRtcUi;

public class ChatWithCallsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        new InfobipRtcUi.Builder(this)
                .enableInAppCalls(
                        () -> Toast.makeText(this, "Calls registration successful!", Toast.LENGTH_SHORT).show(),
                        throwable -> Toast.makeText(this, "Calls registration failed: " + throwable.getMessage(), Toast.LENGTH_SHORT).show()
                ).build();

    }
}