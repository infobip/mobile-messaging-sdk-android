package org.infobip.mobile.messaging.demo;

import android.Manifest;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GoogleApiAvailability;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.GeofenceAreas;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.gcm.PlayServicesSupport;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.Locale;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_EXCEPTION;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_USER_DATA;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static final String TAG = "MainActivity";

    private final BroadcastReceiver userDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.hasExtra(EXTRA_USER_DATA)) {
                showToast(R.string.toast_message_userdata_cannot_save);
            } else {
                UserData userData = UserData.createFrom(intent.getExtras());
                showToast(getString(R.string.toast_message_userdata_set) + ": " + userData.toString());

                if (StringUtils.isBlank(userData.getMsisdn())) {
                    return;
                }

                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putString(ApplicationPreferences.MSISDN, userData.getMsisdn())
                        .apply();
            }
        }
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(ApplicationPreferences.MSISDN)) {
                onMSISDNPreferenceChanged(sharedPreferences);
            }
        }
    };
    private final BroadcastReceiver errorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = getString(R.string.error_api_comm_unknown);
            Throwable exception = (Throwable) intent.getSerializableExtra(EXTRA_EXCEPTION);
            if (exception != null) {
                message = exception.getMessage();
            }

            showToast(message);

            unregisterPreferenceChangeListener();
            readMsisdnFromMobileMessaging();
            registerPreferenceChangeListener();
        }
    };
    private TextView totalReceivedTextView;
    private ExpandableListAdapter listAdapter;
    private final BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = Message.createFrom(intent.getExtras());
            String body = message.getBody();
            Toast.makeText(MainActivity.this, String.format(Locale.getDefault(), getString(R.string.toast_message_received), body), Toast.LENGTH_LONG).show();
            updateCount();
        }
    };
    private boolean receiversRegistered = false;
    private ExpandableListAdapter.OnMessageExpandedListener onMessageExpandedListener = new ExpandableListAdapter.OnMessageExpandedListener() {
        @Override
        public void onMessageExpanded(Message message) {
            MobileMessaging mobileMessaging = MobileMessaging.getInstance(MainActivity.this);
            mobileMessaging.setMessagesSeen(message.getMessageId());
        }
    };

    private final BroadcastReceiver playServicesErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int errorCode = intent.getIntExtra(BroadcastParameter.EXTRA_PLAY_SERVICES_ERROR_CODE, 0);
            if (errorCode == PlayServicesSupport.DEVICE_NOT_SUPPORTED) {
                showDeviceNotSupportedDialog();
                return;
            }

            Dialog errorDialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, errorCode, 0);
            if (errorDialog != null) {
                errorDialog.show();
            }
        }
    };
    private BroadcastReceiver geofenceAreaEnteredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            GeofenceAreas geofenceAreas = GeofenceAreas.createFrom(intent.getExtras());
            Message message = Message.createFrom(intent.getExtras());
            showToast(String.format(Locale.getDefault(), "Message: %s \n triggered for area: %f, %f",
                    message.getBody(), geofenceAreas.getTriggeringLatitude(), geofenceAreas.getTriggeringLongitude()));
        }
    };

    private void showDeviceNotSupportedDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.gps_dialog_title)
                .setMessage(R.string.gps_dialog_message)
                .setCancelable(false)
                .setNeutralButton(R.string.gps_dialog_dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finish();
                    }
                })
                .create()
                .show();
    }

    boolean getNotificationEnabledFromPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean(ApplicationPreferences.NOTIFICATIONS_ENABLED, true);
    }

    private MobileMessaging initializeMobileMessaging() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String permissions[] = {Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            return null;
        }

        MobileMessaging mobileMessaging;
        boolean notificationsEnabled = getNotificationEnabledFromPreferences();
        if (!notificationsEnabled) {
            mobileMessaging = new MobileMessaging.Builder(getApplication())
                    .withMessageStore(SharedPreferencesMessageStore.class)
                    .withoutDisplayNotification()
                    .withGeofencing()
                    .build();
        } else {
            mobileMessaging = new MobileMessaging.Builder(getApplication())
                    .withMessageStore(SharedPreferencesMessageStore.class)
                    .withGeofencing()
                    .withDisplayNotification(new NotificationSettings.Builder(this)
                            .withDefaultIcon(R.drawable.ic_notification)
                            .build())
                    .build();
        }

        readMsisdnFromMobileMessaging();
        updateCount();

        return mobileMessaging;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        initializeMobileMessaging();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        totalReceivedTextView = (TextView) findViewById(R.id.totalReceivedTextView);
        ExpandableListView messagesListView = (ExpandableListView) findViewById(R.id.messagesListView);
        assert messagesListView != null;
        listAdapter = new ExpandableListAdapter(this, onMessageExpandedListener);
        messagesListView.setAdapter(listAdapter);

        initializeMobileMessaging();

        registerReceivers();
        registerPreferenceChangeListener();
        clearNotifications();
    }

    @Override
    protected void onDestroy() {
        unregisterPreferenceChangeListener();
        unregisterReceivers();

        super.onDestroy();
    }

    private void updateCount() {
        if (MobileMessaging.getInstance(this).getMessageStore() == null) {
            return;
        }

        totalReceivedTextView.setText(String.valueOf(MobileMessaging.getInstance(this).getMessageStore().countAll(this)));
        listAdapter.notifyDataSetChanged();
    }

    public void onEraseInboxClick(View view) {
        MobileMessaging.getInstance(this).getMessageStore().deleteAll(this);
        updateCount();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCount();
        clearNotifications();
    }

    private void clearNotifications() {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_inspect) {
            startActivity(new Intent(this, InspectActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showToast(int resId) {
        showToast(getString(resId));
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    private void registerPreferenceChangeListener() {
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    private void unregisterPreferenceChangeListener() {
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    private void onMSISDNPreferenceChanged(SharedPreferences sharedPreferences) {

        Long msisdn = null;
        try {
            if (sharedPreferences.contains(ApplicationPreferences.MSISDN)) {
                msisdn = Long.parseLong(sharedPreferences.getString(ApplicationPreferences.MSISDN, "0"));
                if (msisdn <= 0) {
                    throw new IllegalArgumentException();
                }
            }
        } catch (Exception e) {
            showToast(R.string.toast_message_userdata_invalid);
            return;
        }

        if (msisdn != null) {
            UserData userData = new UserData();
            userData.setMsisdn(msisdn.toString());
            MobileMessaging.getInstance(this).syncUserData(userData, new MobileMessaging.ResultListener<UserData>() {
                @Override
                public void onResult(UserData result) {
                    Log.v(TAG, "User data sync complete: " + result);
                }

                @Override
                public void onError(Throwable e) {
                    Log.e(TAG, "User data sync error: " + e);
                }
            });
        }
    }

    private void registerReceivers() {
        if (!receiversRegistered) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.registerReceiver(errorReceiver,
                    new IntentFilter(Event.API_COMMUNICATION_ERROR.getKey()));
            localBroadcastManager.registerReceiver(userDataReceiver,
                    new IntentFilter(Event.USER_DATA_REPORTED.getKey()));
            localBroadcastManager.registerReceiver(messageReceiver,
                    new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
            localBroadcastManager.registerReceiver(playServicesErrorReceiver,
                    new IntentFilter(Event.GOOGLE_PLAY_SERVICES_ERROR.getKey()));
            localBroadcastManager.registerReceiver(geofenceAreaEnteredReceiver,
                    new IntentFilter(Event.GEOFENCE_AREA_ENTERED.getKey()));
            receiversRegistered = true;
        }
    }

    private void unregisterReceivers() {
        if (receiversRegistered) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.unregisterReceiver(errorReceiver);
            localBroadcastManager.unregisterReceiver(userDataReceiver);
            localBroadcastManager.unregisterReceiver(messageReceiver);
            localBroadcastManager.unregisterReceiver(playServicesErrorReceiver);
            receiversRegistered = false;
        }
    }

    private void readMsisdnFromMobileMessaging() {
        UserData userData = MobileMessaging.getInstance(this).getUserData();
        if (userData == null) {
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .edit()
                .putString(ApplicationPreferences.MSISDN, "" + userData.getMsisdn())
                .apply();
    }
}
