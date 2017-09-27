package org.infobip.mobile.messaging.geo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.common.GoogleApiAvailability;

import org.infobip.mobile.messaging.geo.geofencing.GeofencingHelper;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.common.MAsyncTask;
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
    private GeofencingHelper geofencingHelper;

    @Override
    public void onReceive(final Context context, final Intent intent) {

        if (!GeofencingHelper.isGeoActivated(context)) {
            return;
        }

        final String action = intent.getAction();
        if (StringUtils.isBlank(action)) {
            return;
        }

        // go async to not interfere with UI while processing (max 5-10 seconds for processing)
        final PendingResult result = goAsync();
        MAsyncTask<Void, Void> handleGeoConsistencyActionAsyncTask = new MAsyncTask<Void, Void>() {
            @Override
            public Void run(Void[] voids) {
                MobileMessagingLogger.i(String.format("[%s]", action));
                handleGeoConsistencyAction(context, intent, action);
                result.finish();
                return null;
            }
        };
        handleGeoConsistencyActionAsyncTask.execute();
    }

    private void handleGeoConsistencyAction(Context context, Intent intent, String action) {

        switch (action) {

        /*
         * NETWORK_PROVIDER_ENABLED_ACTION - scheduled 15 seconds after NETWORK_PROVIDER is enabled. Starts monitoring geofences from storage if geo is enabled.
         * SCHEDULED_GEO_REFRESH_ACTION - scheduled to start when campaign needs to be started and area monitored
         * Intent.ACTION_TIME_CHANGED - triggered when system date/time is changed manually (set by user in settings), need to go over all campaigns in this case.
         */
            case NETWORK_PROVIDER_ENABLED_ACTION:
            case SCHEDULED_GEO_REFRESH_ACTION:
            case Intent.ACTION_TIME_CHANGED:
                startGeoMonitoringFromScratch(context);
                break;

        /*
         * This action gets called whenever user deletes data from some app, and we're interested in clear Play Services cleared event
         *  because all registered geofences stop being monitored by GPS in that case.
         */
            case Intent.ACTION_PACKAGE_DATA_CLEARED:
                final Uri data = intent.getData();
                if (data != null && GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE.equals(data.getSchemeSpecificPart())) {
                    startGeoMonitoringFromScratch(context);
                }
                break;

        /*
         * Scheduled to be invoked when first area from geo storage needs to expire. In that case GPS stop monitoring areas, but we
         *  also need to be aware of this event.
         */
            case SCHEDULED_GEO_EXPIRE_ACTION:
                geofencingHelper(context).removeExpiredAreas();
                break;
        }
    }

    private void startGeoMonitoringFromScratch(Context context) {
        GeofencingHelper.setAllActiveGeoAreasMonitored(context, false);

        if (geofencingHelper(context).isLocationEnabled(context)) {
            geofencingHelper(context).startGeoMonitoringIfNecessary();
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
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags);

        // not using exact setter with wake lock because we don't want to wake up device that sleeps
        alarmManager.set(alarmType, when.getTime(), pendingIntent);
    }

    public GeofencingHelper geofencingHelper(Context context) {
        if (geofencingHelper == null) {
            geofencingHelper = new GeofencingHelper(context);
        }
        return geofencingHelper;
    }
}