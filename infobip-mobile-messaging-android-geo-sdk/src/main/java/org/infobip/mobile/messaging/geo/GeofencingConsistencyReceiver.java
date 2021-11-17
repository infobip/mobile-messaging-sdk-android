package org.infobip.mobile.messaging.geo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.Date;

/**
 * This class persists geo monitoring consistency. Consistency can be broken in several cases, referring to
 * https://developer.android.com/training/location/geofencing.html#BestPractices (paragraph "Re-register geofences only when required"):
 * <p>
 * <i>The app must re-register geofences if they're still needed after the following events, since the system cannot recover the geofences in the following cases:</i>
 * <ul>
 * <li>The device is rebooted. The app should listen for the device's boot complete action, and then re- register the geofences required.</li>
 * <li>The app is uninstalled and re-installed.</li>
 * <li>The app's data is cleared.</li>
 * <li>Google Play services data is cleared.</li>
 * <li>The app has received a GEOFENCE_NOT_AVAILABLE alert. This typically happens after NLP (Android's Network Location Provider) is disabled.</li>
 * </ul>
 *
 * @author tjuric
 * @since 20/02/17.
 */
public class GeofencingConsistencyReceiver extends BroadcastReceiver {

    public static final String NETWORK_PROVIDER_ENABLED_ACTION = "org.infobip.mobile.messaging.geo.intent.NETWORK_PROVIDER_ENABLED";
    public static final String SCHEDULED_GEO_REFRESH_ACTION = "org.infobip.mobile.messaging.geo.intent.SCHEDULED_GEO_REFRESH";
    public static final String SCHEDULED_GEO_EXPIRE_ACTION = "org.infobip.mobile.messaging.geo.intent.SCHEDULED_GEO_EXPIRE";

    @Override
    public void onReceive(final Context context, final Intent intent) {

        if (!GeofencingHelper.isGeoActivated(context)) {
            return;
        }

        final String action = intent.getAction();
        if (StringUtils.isBlank(action)) {
            return;
        }

        switch (action) {
            case NETWORK_PROVIDER_ENABLED_ACTION:
            case SCHEDULED_GEO_REFRESH_ACTION:
            case Intent.ACTION_TIME_CHANGED:
            case Intent.ACTION_PACKAGE_DATA_CLEARED:
            case SCHEDULED_GEO_EXPIRE_ACTION:
                GeofencingConsistencyIntentService.enqueueWork(context, intent);
                break;
        }
    }

    public static void scheduleConsistencyAlarm(Context context, int alarmType, Date when, String action, int flags) {
        if (when == null) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, GeofencingConsistencyReceiver.class);
        intent.setAction(action);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags = flags | PendingIntent.FLAG_MUTABLE;
        }

        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);

        // not using exact setter with wake lock because we don't want to wake up device that sleeps
        alarmManager.set(alarmType, when.getTime(), pendingIntent);
    }
}