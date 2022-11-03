package org.infobip.mobile.messaging.demo;

import static org.infobip.mobile.messaging.api.support.util.StringUtils.isBlank;
import static org.infobip.mobile.messaging.util.StringUtils.isNotBlank;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.SuccessPending;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserIdentity;
import org.infobip.mobile.messaging.inbox.Inbox;
import org.infobip.mobile.messaging.inbox.InboxBundleMapper;
import org.infobip.mobile.messaging.inbox.MobileInbox;
import org.infobip.mobile.messaging.inbox.MobileInboxEvent;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;

public class MainActivity extends AppCompatActivity {

    private EditText etExtUsrId;
    private Button btnPersonalize;
    private Button btnDepersonalize;
    private BadgeDrawable badgeDrawable;
    private Button btnToInbox;

    private MobileMessaging mobileMessaging;
    private SharedPreferences sharedPref;
    private String externalUserId;
    private Inbox inbox;
    private Boolean receiversRegistered = false;

    private final BroadcastReceiver sdkBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateControls();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setSupportActionBar(this.findViewById(R.id.toolbar));

        btnToInbox = findViewById(R.id.btn_to_inbox);
        etExtUsrId = findViewById(R.id.etExtUserId);
        btnPersonalize = findViewById(R.id.btn_personalize);
        btnDepersonalize = findViewById(R.id.btn_depersonalize);

        registerReceivers();
        mobileMessaging = MobileMessaging.getInstance(this);

        // Setting onClick listeners for buttons
        btnPersonalize.setOnClickListener(this::onPersonalizeButtonPressed);
        btnDepersonalize.setOnClickListener(this::onDepersonalizeButtonPressed);
        btnToInbox.setOnClickListener(onToInboxButtonPressed());

        // Setting the badge
        badgeDrawable = BadgeDrawable.create(this);
        btnToInbox.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @SuppressLint({"UnsafeOptInUsageError", "UnsafeExperimentalUsageError"})
            @Override
            public void onGlobalLayout() {
                badgeDrawable.setNumber((inbox == null) ? 0 : inbox.getCountUnread());
                badgeDrawable.setHorizontalOffset(30);
                badgeDrawable.setVerticalOffset(20);
                BadgeUtils.attachBadgeDrawable(badgeDrawable, btnToInbox, null);
                btnToInbox.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        sharedPref = getSharedPreferences(Constants.SHARED_PREF_KEY, Context.MODE_PRIVATE);

        // Checking if there is inbox in passed arguments from second activity
        Intent intent = getIntent();
        if (intent != null) {
            Bundle inboxValue = intent.getBundleExtra(Constants.BUNDLE_KEY_DEMO_INBOX);
            if (inboxValue != null) {
                inbox = Inbox.createFrom(inboxValue);
                saveInboxToSharedPref();
            } else {
                // Checking if there is a saved inbox in preferences
                getInboxFromSharedPref();
            }
        }

        // Populating text field with external user id if present
        if (isUserPersonalizedWithExternalUserId()) {
            externalUserId = mobileMessaging.getUser().getExternalUserId();
            etExtUsrId.setText(externalUserId);
        } else {
            etExtUsrId.setHint(R.string.extuserid_hint);
        }

        updateControls();
    }

    private void getInboxFromSharedPref() {
        if (isUserPersonalizedWithExternalUserId()) {
            String sharedPrefString = sharedPref.getString(Constants.SHARED_PREF_INBOX_VALUE, "");
            if (isNotBlank(sharedPrefString)) {
                inbox = new Inbox().fromString(sharedPrefString);
            }
        }
    }

    private void saveInboxToSharedPref() {
        SharedPreferences.Editor sharedPrefEditor = sharedPref.edit()
                .putString(Constants.SHARED_PREF_INBOX_VALUE, inbox.toString());
        sharedPrefEditor.commit();
    }

    // Personalizing by external user id provided, then fetching inbox
    private void onPersonalizeButtonPressed(View view) {
        UserIdentity userIdentity = new UserIdentity();
        String extUserId = etExtUsrId.getText().toString();
        if (isBlank(extUserId)) {
            Toast.makeText(MainActivity.this, R.string.cannot_personalize, Toast.LENGTH_SHORT).show();
            return;
        }
        userIdentity.setExternalUserId(extUserId);
        mobileMessaging.personalize(userIdentity, null, new MobileMessaging.ResultListener<User>() {
            @Override
            public void onResult(Result<User, MobileMessagingError> result) {
                externalUserId = extUserId;
                Toast.makeText(MainActivity.this, R.string.personalized_success, Toast.LENGTH_SHORT).show();
                prepareInbox();
            }
        });
    }

    public void onDepersonalizeButtonPressed(View view) {
        //Cleaning up inbox because new user might log in
        inbox = new Inbox();
        externalUserId = null;
        saveInboxToSharedPref();
        mobileMessaging.depersonalize(new MobileMessaging.ResultListener<SuccessPending>() {
            @Override
            public void onResult(Result<SuccessPending, MobileMessagingError> result) {
                Toast.makeText(MainActivity.this, R.string.depersonalized_success, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View.OnClickListener onToInboxButtonPressed() {
        return view -> {
            // Passing external user id and inbox as arguments to the second activity
            Bundle arguments = new Bundle();
            arguments.putString(Constants.BUNDLE_KEY_EXTERNAL_USER_ID, externalUserId);
            arguments.putBundle(Constants.BUNDLE_KEY_DEMO_INBOX, InboxBundleMapper.inboxToBundle(inbox));

            Intent intent = new Intent(MainActivity.this, InboxActivity.class);
            intent.putExtras(arguments);
            MainActivity.this.startActivity(intent);
        };
    }

    private boolean isUserPersonalizedWithExternalUserId() {
        if (mobileMessaging.getUser() != null)
            return isNotBlank(mobileMessaging.getUser().getExternalUserId());
        return false;
    }

    private boolean isPushRegistrationAvailable() {
        return mobileMessaging.getInstallation() != null;
    }

    private void prepareInbox() {
        MobileInbox.getInstance(this).fetchInbox(mobileMessaging.getUser().getExternalUserId(), null, new MobileMessaging.ResultListener<Inbox>() {
            @Override
            public void onResult(Result<Inbox, MobileMessagingError> result) {
                if (result.isSuccess()) {
                    inbox = result.getData();
                    saveInboxToSharedPref();
                    updateBadge();
                }
            }
        });
    }

    // Changing buttons & text field availability
    private void updateControls() {
        updateBadge();
        etExtUsrId.setEnabled(isPushRegistrationAvailable() && !isUserPersonalizedWithExternalUserId());
        btnPersonalize.setEnabled(!isUserPersonalizedWithExternalUserId());
        btnDepersonalize.setEnabled(isUserPersonalizedWithExternalUserId());
        btnToInbox.setEnabled(isUserPersonalizedWithExternalUserId());
    }

    // Showing count of unread inbox messages
    private void updateBadge() {
        badgeDrawable.setNumber((inbox == null) ? 0 : inbox.getCountUnread());
    }

    @Override
    public void onDestroy() {
        unregisterReceivers();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        unregisterReceivers();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBadge();
    }

    private void registerReceivers() {
        if (!receiversRegistered) {
            LocalBroadcastManager
                    .getInstance(this)
                    .registerReceiver(sdkBroadcastReceiver, new IntentFilter() {{
                        addAction(Event.PERSONALIZED.getKey());
                        addAction(Event.DEPERSONALIZED.getKey());
                        addAction(MobileInboxEvent.INBOX_MESSAGES_FETCHED.getKey());
                    }});
        }
        this.receiversRegistered = true;
    }

    private void unregisterReceivers() {
        if (receiversRegistered) {
            LocalBroadcastManager
                    .getInstance(this)
                    .unregisterReceiver(sdkBroadcastReceiver);
        }
        this.receiversRegistered = false;
    }
}
