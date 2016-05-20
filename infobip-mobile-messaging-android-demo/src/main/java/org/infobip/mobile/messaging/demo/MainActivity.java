package org.infobip.mobile.messaging.demo;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;

import static org.infobip.mobile.messaging.BroadcastParameter.*;

public class MainActivity extends AppCompatActivity {
    private TextView totalReceivedTextView;
    private ExpandableListAdapter listAdapter;
    private boolean receiversRegistered = false;

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = new Message(intent.getExtras());
            String body = message.getBody();
            Toast.makeText(MainActivity.this, "Message received: " + body, Toast.LENGTH_LONG).show();
            updateCount();
        }
    };
    private final BroadcastReceiver validationErrorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String parameterName = intent.getStringExtra(EXTRA_PARAMETER_NAME);
            if (parameterName.equals(EXTRA_PARAMETER_MSISDN)) {
                Throwable throwable = (Throwable)intent.getSerializableExtra(EXTRA_PARAMETER_EXCEPTION);
                long msisdn = intent.getLongExtra(EXTRA_PARAMETER_VALUE, 0);
                showToast(throwable.getMessage() + " for " + msisdn);

                unregisterPreferenceChangeListener();
                readMsisdnFromMobileMessaging();
                registerPreferenceChangeListener();
            }
        }
    };
    private final BroadcastReceiver msisdnRecevier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long msisdn = intent.getLongExtra(EXTRA_PARAMETER_MSISDN, 0);
            if (msisdn == 0) {
                showToast(R.string.toast_message_msisdn_cannot_save);
            } else {
                showToast(getString(R.string.toast_message_msisdn_set) + ": " + msisdn);
            }
        }
    };
    private ExpandableListAdapter.OnMessageExpandedListener onMessageExpandedListener = new ExpandableListAdapter.OnMessageExpandedListener() {
        @Override
        public void onMessageExpanded(Message message) {
            MobileMessaging mobileMessaging = MobileMessaging.getInstance(MainActivity.this);
            mobileMessaging.setMessagesSeen(message.getMessageId());
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new MobileMessaging.Builder(this)
                .withMessageStore(SharedPreferencesMessageStore.class)
                .build();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        totalReceivedTextView = (TextView) findViewById(R.id.totalReceivedTextView);

        ExpandableListView messagesListView = (ExpandableListView) findViewById(R.id.messagesListView);
        assert messagesListView != null;
        listAdapter = new ExpandableListAdapter(this, onMessageExpandedListener);
        messagesListView.setAdapter(listAdapter);

        readMsisdnFromMobileMessaging();
        registerReceivers();
        registerPreferenceChangeListener();
        updateCount();
        clearNotifications();
    }

    @Override
    protected void onDestroy() {
        unregisterPreferenceChangeListener();
        unregisterReceivers();

        super.onDestroy();
    }

    private void updateCount() {
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
        try {
            if (sharedPreferences.contains(ApplicationPreferences.MSISDN)) {
                long msisdn = Long.parseLong(sharedPreferences.getString(ApplicationPreferences.MSISDN, "0"));
                if (msisdn > 0) {
                    MobileMessaging mobileMessaging = MobileMessaging.getInstance(MainActivity.this);
                    mobileMessaging.setMsisdn(msisdn);
                } else {
                    throw new IllegalArgumentException();
                }
            }
        } catch (Exception e) {
            showToast(R.string.toast_message_msisdn_invalid);
        }
    }

    private void registerReceivers() {
        if (!receiversRegistered) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.registerReceiver(validationErrorReceiver,
                    new IntentFilter(Event.API_PARAMETER_VALIDATION_ERROR.getKey()));
            localBroadcastManager.registerReceiver(msisdnRecevier,
                    new IntentFilter(Event.MSISDN_SYNCED.getKey()));
            localBroadcastManager.registerReceiver(messageReceiver,
                    new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
            receiversRegistered = true;
        }
    }

    private void unregisterReceivers() {
        if (receiversRegistered) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.unregisterReceiver(validationErrorReceiver);
            localBroadcastManager.unregisterReceiver(msisdnRecevier);
            localBroadcastManager.unregisterReceiver(messageReceiver);
            receiversRegistered = false;
        }
    }

    private void readMsisdnFromMobileMessaging() {
        PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                .edit()
                .putString(ApplicationPreferences.MSISDN, "" + MobileMessaging.getInstance(this).getMsisdn())
                .apply();
    }
}
