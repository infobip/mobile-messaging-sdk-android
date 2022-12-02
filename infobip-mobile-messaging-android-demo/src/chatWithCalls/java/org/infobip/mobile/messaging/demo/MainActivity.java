package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.textfield.TextInputEditText;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.SuccessPending;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserAttributes;
import org.infobip.mobile.messaging.UserIdentity;
import org.infobip.mobile.messaging.chat.InAppChat;
import org.infobip.mobile.messaging.chat.view.InAppChatFragment;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity implements InAppChatFragment.InAppChatActionBarProvider {

    private boolean pushRegIdReceiverRegistered = false;
    private final BroadcastReceiver pushRegIdReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String pushRegId = intent.getStringExtra(BroadcastParameter.EXTRA_INFOBIP_ID);
                showPushRegId(pushRegId);
            }
        }
    };

    /* InAppChatActionBarProvider */
    @Nullable
    @Override
    public ActionBar getOriginalSupportActionBar() {
        return getSupportActionBar();
    }

    @Override
    public void onInAppChatBackPressed() {
        InAppChat.getInstance(MainActivity.this).hideInAppChatFragment(getSupportFragmentManager());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InAppChat.getInstance(this).activate();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setUpPushRegIdField();
        setUpOpenChatButton();
        setUpPersonalizationButton();
        setUpDepersonalizationButton();
    }

    @Override
    protected void onDestroy() {
        if (this.pushRegIdReceiverRegistered) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(pushRegIdReceiver);
            } catch (Throwable t) {
                Log.e("MainActivity", "Unable to unregister pushRegIdReceiverRegistered", t);
            }
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.copy_id_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.copy_id) {
            setPushRegIdToClipboard(getPushRegId());
            return true;
        }
        return false;
    }

    private void setUpPushRegIdField() {
        String pushRegId = getPushRegId();
        if (!showPushRegId(pushRegId)) {
            LocalBroadcastManager.getInstance(this).registerReceiver(pushRegIdReceiver, new IntentFilter(Event.REGISTRATION_CREATED.getKey()));
            this.pushRegIdReceiverRegistered = true;
        }
    }

    private void setPushRegIdToClipboard(String pushRegId) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.action_registration_id_copy), pushRegId);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, R.string.toast_registration_id_copy, Toast.LENGTH_SHORT).show();
    }

    private boolean showPushRegId(String pushRegId) {
        if (StringUtils.isNotBlank(pushRegId)) {
            TextInputEditText pushRegIdEditText = findViewById(R.id.pushRegIdEditText);
            pushRegIdEditText.setText(pushRegId);
            pushRegIdEditText.setKeyListener(null);
            pushRegIdEditText.setOnClickListener(view -> {
                setPushRegIdToClipboard(pushRegId);
            });
            return true;
        }
        return false;
    }

    private String getPushRegId() {
        return MobileMessaging.getInstance(this).getInstallation().getPushRegistrationId();
    }

    private void setUpOpenChatButton() {
        Button openChat = findViewById(R.id.openChat);
        openChat.setOnClickListener((v) -> {
            //Shows in-app chat as Activity
            InAppChat.getInstance(MainActivity.this).inAppChatView().show();
        });
    }

    private void setUpPersonalizationButton() {
        Button personalize = findViewById(R.id.personalize);
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setPhones(new HashSet<>(Collections.singletonList("79210000000")));
        userIdentity.setEmails(new HashSet<>(Collections.singletonList("android.chat.demo@infobip.com")));

        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFirstName("Android");
        userAttributes.setMiddleName("Chat");
        userAttributes.setLastName("Demo");
        personalize.setOnClickListener((v) -> MobileMessaging.getInstance(this).personalize(
                userIdentity,
                userAttributes,
                true,
                new MobileMessaging.ResultListener<User>() {
                    @Override
                    public void onResult(Result<User, MobileMessagingError> result) {
                        if (result.isSuccess())
                            Toast.makeText(MainActivity.this, "Personalization done!", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Personalization failed: " + result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        ));
    }

    private void setUpDepersonalizationButton() {
        Button depersonalize = findViewById(R.id.depersonalize);
        depersonalize.setOnClickListener((v) -> MobileMessaging.getInstance(this).depersonalize(
                new MobileMessaging.ResultListener<SuccessPending>() {
                    @Override
                    public void onResult(Result<SuccessPending, MobileMessagingError> result) {
                        if (result.isSuccess())
                            Toast.makeText(MainActivity.this, "Depersonalization done!", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Depersonalization failed: " + result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        ));
    }
}
