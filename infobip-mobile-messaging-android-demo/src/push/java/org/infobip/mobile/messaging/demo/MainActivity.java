package org.infobip.mobile.messaging.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.infobip.mobile.messaging.geo.MobileGeo;
import org.infobip.mobile.messaging.storage.MessageStore;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private MobileMessaging mobileMessaging;
    private MessageStore messageStore;
    private TextView totalReceivedTextView;
    private ArrayAdapter<String> adapter;
    private Menu menu;

    private BroadcastReceiver sdkBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(this.<Toolbar>findViewById(R.id.toolbar));

        totalReceivedTextView = findViewById(R.id.tv_received_messages_number);
        adapter = new ArrayAdapter<>(this, R.layout.message_row, R.id.tv_message_text);
        mobileMessaging = MobileMessaging.getInstance(this);
        messageStore = mobileMessaging.getMessageStore();

        ListView lv = findViewById(R.id.lv_messages);
        lv.setAdapter(adapter);

        activateGeofencing();
        refresh();
        clearNotifications();

        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(sdkBroadcastReceiver, new IntentFilter() {{
                    addAction(Event.MESSAGE_RECEIVED.getKey());
                    addAction(Event.PRIMARY_CHANGED.getKey());
                }});
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(sdkBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
        clearNotifications();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            activateGeofencing();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        updatePrimaryInMenu();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_erase:
                actionErase();
                return true;

            case R.id.action_gsm:
                actionGsm();
                return true;

            case R.id.action_registration_id:
                actionRegistrationId();
                return true;

            case R.id.action_primary:
                actionPrimary();
                return true;

            case R.id.action_depersonalize:
                actionDepersonalize();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void actionRegistrationId() {
        showDialog(R.string.dialog_title_registration_id, mobileMessaging.getPushRegistrationId(), null);
    }

    private void actionGsm() {
        final UserData userData = mobileMessaging.getUser() != null ? mobileMessaging.getUser() : new UserData();
        String gsm = userData.getGsms() == null || userData.getGsms().isEmpty() ? "" : userData.getGsms().iterator().next();
        showDialog(R.string.dialog_title_gsm, gsm, new RunnableWithParameter<String>() {
            @Override
            public void run(String param) {
                userData.setGsms(CollectionUtils.setOf(param));
                mobileMessaging.saveUser(userData, new MobileMessaging.ResultListener<UserData>() {
                    @Override
                    public void onResult(UserData result) {
                        Toast.makeText(MainActivity.this, R.string.toast_user_data_saved, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void actionErase() {
        messageStore.deleteAll(this);
        refresh();
    }

    private void activateGeofencing() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            String permissions[] = {Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        MobileGeo.getInstance(this).activateGeofencing();
    }

    private void clearNotifications() {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }

    private void refresh() {
        totalReceivedTextView.setText(String.valueOf(messageStore.countAll(this)));
        adapter.clear();
        for (Message message : messageStore.findAll(this)) {
            adapter.addAll(message.getBody());
        }
        adapter.notifyDataSetChanged();
        updatePrimaryInMenu();
    }

    private void showDialog(int titleResId, String text, @Nullable final RunnableWithParameter<String> onSaved) {
        @SuppressLint("InflateParams")
        View v = getLayoutInflater().inflate(R.layout.dialog_text, null);
        final EditText et = v.findViewById(R.id.et_dialog_text);
        et.setText(text);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setView(v)
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setTitle(titleResId);

        if (onSaved != null) {
            builder.setPositiveButton(R.string.dialog_action_save, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onSaved.run(et.getText().toString());
                    dialog.dismiss();
                }
            });
        }

        builder.show();
    }

    private void actionPrimary() {
        mobileMessaging.setCurrentInstallationAsPrimary(!mobileMessaging.isPrimaryDevice());
    }

    private void actionDepersonalize() {
        mobileMessaging.depersonalize();
    }

    private void updatePrimaryInMenu() {
        if (menu == null) {
            return;
        }

        MenuItem item = menu.findItem(R.id.action_primary);
        if (mobileMessaging.isPrimaryDevice()) {
            item.setTitle(R.string.menu_action_disable_primary);
        } else {
            item.setTitle(R.string.menu_action_enable_primary);
        }
    }
}