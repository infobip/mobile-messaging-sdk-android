package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;

public class MainActivity extends AppCompatActivity {
    private boolean isReceiverRegistered;
    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = new Message(intent.getExtras());
            String body = message.getBody();
            Toast.makeText(MainActivity.this, "Message received: " + body, Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new MobileMessaging.Builder(this)
                .withCallbackActivity(MainActivity.class)
                .withApplicationId(getResources().getString(R.string.infobip_api_key))
                .withGcmSenderId(getResources().getString(R.string.google_app_id))
                .withDefaultTitle(getResources().getString(R.string.app_name))
//                .withDefaultIcon(R.drawable.cast_ic_notification_0)
//                .withDisplayNotification()
//                .withoutDisplayNotification()
//                .withApiUri("http://10.116.52.238:18080")
                .withApiUri("http://oneapi.ioinfobip.com")
                .build();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "RegId: " + MobileMessaging.getInstance().getRegistrationId(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        registerReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
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
