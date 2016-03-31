package org.infobip.mobile.messaging.demo;

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
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import io.fabric.sdk.android.Fabric;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.storage.SharedPreferencesMessageStore;

public class MainActivity extends AppCompatActivity {
    private boolean isReceiverRegistered;
    private TextView totalReceivedTextView;
    private ExpandableListView messagesListView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Crashlytics crashlyticsKit = new Crashlytics.Builder()
                .core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build();

        // Initialize Fabric with the debug-disabled crashlytics.
        Fabric.with(this, crashlyticsKit);

        new MobileMessaging.Builder(this)
                .withCallbackActivity(MainActivity.class)
                .withApplicationCode(getResources().getString(R.string.infobip_application_code))
                .withGcmSenderId(getResources().getString(R.string.google_app_id))
                .withDefaultTitle(getResources().getString(R.string.app_name))
                .withDefaultIcon(R.mipmap.ic_launcher)
//                .withDisplayNotification()
//                .withoutDisplayNotification()
//                .withApiUri("http://10.116.52.238:18080")
//                .withApiUri("https://oneapi.ioinfobip.com")
                .withMessageStore(SharedPreferencesMessageStore.class)
//                .withoutMessageStore()
                .build();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        totalReceivedTextView = (TextView) findViewById(R.id.totalReceivedTextView);
        messagesListView = (ExpandableListView) findViewById(R.id.messagesListView);
        listAdapter = new ExpandableListAdapter(this);
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
        NotificationManager notifManager= (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.cancelAll();
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
