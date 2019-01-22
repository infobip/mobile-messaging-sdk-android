package org.infobip.mobile.messaging.demo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.chat.ChatMessageStorage;
import org.infobip.mobile.messaging.chat.MobileChat;

/**
 * @author sslavin
 * @since 13/11/2017.
 */

public class MainActivity extends AppCompatActivity {

    private MobileChat mobileChat;
    private ChatMessageStorage messageStorage;
    private ChatMessagesAdapter messagesAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(this.<Toolbar>findViewById(R.id.toolbar));

        mobileChat = MobileChat.getInstance(this);
        messageStorage = mobileChat.getChatMessageStorage();

        RecyclerView rv = findViewById(R.id.rv_messages);
        rv.setLayoutManager(new LinearLayoutManager(this));

        messagesAdapter = new ChatMessagesAdapter(this, messageStorage.findAllMessages());
        messageStorage.registerListener(messagesAdapter);
        rv.setAdapter(messagesAdapter);

        Button btn = findViewById(R.id.btn_send);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText et = findViewById(R.id.et_message_text);
                String text = et.getText().toString();
                et.setText("");
                if (!TextUtils.isEmpty(text)) {
                    mobileChat.sendMessage(text);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        messageStorage.unregisterListener(messagesAdapter);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_id) {
            copyRegistrationIdToClipboard();
            Toast.makeText(this, R.string.toast_registration_id_copy, Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("ConstantConditions")
    private void copyRegistrationIdToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Registration ID", MobileMessaging.getInstance(this).getInstallation().getPushRegistrationId());
        clipboard.setPrimaryClip(clip);
    }
}
