package org.infobip.mobile.messaging.demo;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.storage.MessageStore;

public class MainActivity extends AppCompatActivity {

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

        refresh();
        clearNotifications();

        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(sdkBroadcastReceiver, new IntentFilter() {{
                    addAction(Event.MESSAGE_RECEIVED.getKey());
                    addAction(Event.INSTALLATION_UPDATED.getKey());
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

            case R.id.action_phone:
                actionPhone();
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
        showDialog(R.string.dialog_title_registration_id, mobileMessaging.getInstallation().getPushRegistrationId(), null);
    }

    private void actionPhone() {
        final User user = mobileMessaging.getUser() != null ? mobileMessaging.getUser() : new User();
        String phone = user.getPhones() == null || user.getPhones().isEmpty() ? "" : user.getPhones().iterator().next();
        showDialog(R.string.dialog_title_gsm, phone, param -> {
            user.setPhones(CollectionUtils.setOf(param));
            mobileMessaging.saveUser(user, new MobileMessaging.ResultListener<User>() {
                @Override
                public void onResult(Result<User, MobileMessagingError> result) {
                    if (result.isSuccess()) {
                        Toast.makeText(MainActivity.this, R.string.toast_user_data_saved, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private void actionErase() {
        messageStore.deleteAll(this);
        refresh();
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
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setTitle(titleResId);

        if (onSaved != null) {
            builder.setPositiveButton(R.string.dialog_action_save, (dialog, which) -> {
                onSaved.run(et.getText().toString());
                dialog.dismiss();
            });
        }

        builder.show();
    }

    private void actionPrimary() {
        Installation installation = new Installation();
        installation.setPrimaryDevice(!mobileMessaging.getInstallation().isPrimaryDevice());
        mobileMessaging.saveInstallation(installation);
    }

    private void actionDepersonalize() {
        mobileMessaging.depersonalize();
    }

    private void updatePrimaryInMenu() {
        if (menu == null) {
            return;
        }

        MenuItem item = menu.findItem(R.id.action_primary);
        if (mobileMessaging.getInstallation().isPrimaryDevice()) {
            item.setTitle(R.string.menu_action_disable_primary);
        } else {
            item.setTitle(R.string.menu_action_enable_primary);
        }
    }
}