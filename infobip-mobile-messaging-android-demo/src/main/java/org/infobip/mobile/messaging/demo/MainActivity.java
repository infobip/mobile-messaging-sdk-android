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
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;

import java.util.Locale;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_EXCEPTION;
import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_USER_DATA;

public class MainActivity extends AppCompatActivity implements MobileMessaging.OnReplyClickListener {
    private final BroadcastReceiver userDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.hasExtra(EXTRA_USER_DATA)) {
                showToast(R.string.toast_message_userdata_cannot_save);
            } else {
                UserData userData = new UserData(intent.getStringExtra(EXTRA_USER_DATA));
                showToast(getString(R.string.toast_message_userdata_set) + ": " + userData.toString());
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
            Message message = new Message(intent.getExtras());
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new MobileMessaging.Builder(this)
                .withOnReplyClickListener(this)
                .withMessageStore(SharedPreferencesMessageStore.class)
                .withDisplayNotification(new NotificationSettings.Builder(this)
                        .withDefaultIcon(R.drawable.ic_notification)
                        .withMarkSeenActionTitle(getString(R.string.action_mark_seen))
                        .withReplyActionTitle(getString(R.string.action_reply))
                        .withCouponUrlActionTitle(getString(R.string.action_coupon))
                        .withoutForegroundNotification()
                        .build())
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

        String userId = sharedPreferences.getString(ApplicationPreferences.USER_ID, null);
        if (userId == null) {
            showToast(R.string.toast_missing_user_id);
            return;
        }

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
            MobileMessaging.getInstance(this).setUserData(userId, userData);
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
            receiversRegistered = true;
        }
    }

    private void unregisterReceivers() {
        if (receiversRegistered) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.unregisterReceiver(errorReceiver);
            localBroadcastManager.unregisterReceiver(userDataReceiver);
            localBroadcastManager.unregisterReceiver(messageReceiver);
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

    @Override
    public void onReplyClicked(Intent intent) {
        Intent replyIntent = new Intent(this, ReplyActivity.class);
        String messageExtra = MobileMessagingProperty.EXTRA_MESSAGE.getKey();
        Bundle bundle = intent.getBundleExtra(messageExtra);
        if (bundle != null) {
            replyIntent.putExtra(messageExtra, bundle);
            startActivity(replyIntent);
        }
    }
}
