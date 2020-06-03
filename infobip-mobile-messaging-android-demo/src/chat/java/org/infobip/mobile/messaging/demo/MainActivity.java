package org.infobip.mobile.messaging.demo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.chat.InAppChat;

/**
 * @author sslavin
 * @since 13/11/2017.
 */

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(this.<Toolbar>findViewById(R.id.toolbar));

        Button btn = findViewById(R.id.btn_open);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InAppChat.getInstance(MainActivity.this).inAppChatView().show();

            }
        });
    }

    @Override
    protected void onDestroy() {
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
