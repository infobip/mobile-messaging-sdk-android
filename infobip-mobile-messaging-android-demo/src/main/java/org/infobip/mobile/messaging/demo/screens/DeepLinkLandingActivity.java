package org.infobip.mobile.messaging.demo.screens;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sslavin
 * @since 31/08/2017.
 */

public class DeepLinkLandingActivity extends AppCompatActivity {

    private static final Map<String, Class<? extends Activity>> activityMap = new HashMap<String, Class<? extends Activity>>() {{
        put("redscreen", RedScreenActivity.class);
        put("greenscreen", GreenScreenActivity.class);
        put("bluescreen", BlueScreenActivity.class);
    }};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        processDeepLink();
        finish();
    }

    private void processDeepLink() {
        Intent intent = getIntent();
        if (intent == null || intent.getData() == null) {
            return;
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        List<String> segments = intent.getData().getPathSegments();
        if (segments.isEmpty()) {
            Intent launcherIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launcherIntent != null) {
                startActivity(launcherIntent.putExtras(intent));
            }
            return;
        }

        for (String segment : intent.getData().getPathSegments()) {
            if (activityMap.containsKey(segment)) {
                Intent nextIntent = new Intent(this, activityMap.get(segment)).putExtras(intent);
                stackBuilder.addNextIntent(nextIntent);
            }
        }
        stackBuilder.startActivities();
    }
}
