package org.infobip.mobile.messaging.demo;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;

public class MainActivity extends AppCompatActivity {
    private boolean isReceiverRegistered;
    private TextView totalReceivedTextView;
    private ExpandableListAdapter listAdapter;
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = new Message(intent.getExtras());
            String body = message.getBody();
            Toast.makeText(MainActivity.this, "Message received: " + body, Toast.LENGTH_LONG).show();
            updateCount();
        }
    };
    private ExpandableListAdapter.OnMessageExpandedListener onMessageExpandedListener = new ExpandableListAdapter.OnMessageExpandedListener() {
        @Override
        public void onMessageExpanded(Message message) {
            MobileMessaging mobileMessaging = MobileMessaging.getInstance(MainActivity.this);
            if (mobileMessaging.isMessageStoreEnabled()) {
                message.setSeenTimestamp(System.currentTimeMillis());
                mobileMessaging.getMessageStore().save(MainActivity.this, message);
            }
            mobileMessaging.addUnreportedSeenMessageIds(message.getMessageId());
            mobileMessaging.reportUnreportedSeenMessageIds();
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

        registerReceiver();
        updateCount();
        clearNotifications();
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
        registerReceiver();
        updateCount();
        clearNotifications();
    }

    private void clearNotifications() {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    private void registerReceiver() {
        if (!isReceiverRegistered) {

            LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver,
                    new IntentFilter(Event.MESSAGE_RECEIVED.getKey()));
            isReceiverRegistered = true;
        }
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
}
