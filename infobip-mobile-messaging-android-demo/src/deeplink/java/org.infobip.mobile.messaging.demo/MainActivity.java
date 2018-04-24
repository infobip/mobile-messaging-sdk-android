package org.infobip.mobile.messaging.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sslavin
 * @since 08/11/2017.
 */

public class MainActivity extends AppCompatActivity {

    private static final Map<String, Class<? extends Activity>> activityMap = new HashMap<String, Class<? extends Activity>>() {{
        put("redScreen", RedScreenActivity.class);
        put("greenScreen", GreenScreenActivity.class);
        put("blueScreen", BlueScreenActivity.class);
    }};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(this.<Toolbar>findViewById(R.id.toolbar));
        processDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processDeepLink(intent);
    }

    private void processDeepLink(Intent intent) {
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
