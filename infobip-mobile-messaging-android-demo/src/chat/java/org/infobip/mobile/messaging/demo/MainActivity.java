package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.SuccessPending;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.chat.InAppChat;
import org.infobip.mobile.messaging.chat.view.InAppChatFragment;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.util.StringUtils;
import org.json.JSONObject;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * @author sslavin
 * @since 13/11/2017.
 */

public class MainActivity extends AppCompatActivity implements InAppChatFragment.InAppChatActionBarProvider {

    private final String TAG = "DemoApp";
    private final String EXTRA_AUTH_DATA = "org.infobip.mobile.messaging.demo.MainActivity.EXTRA_AUTH_DATA";
    /**
     * Widget ID
     * <p>
     * Widget ID is used for generating JWT token to be able use InAppChat as authenticated customer.
     * You can get your widget ID in widget configuration page.
     */
    private final String WIDGET_ID = "your_widget_id";
    /**
     * Widget secret key in JSON form
     * <p>
     * Secret key is used for generating JWT token to be able use InAppChat as authenticated customer.
     * You can generate new secret key following a guide https://www.infobip.com/docs/live-chat/user-types#enable-authenticated-customers.
     */
    private final String WIDGET_SECRET_KEY_JSON = "your_widget_secret_key";
    private final InAppChat inAppChat = InAppChat.getInstance(this);
    private boolean pushRegIdReceiverRegistered = false;
    private JWTSubjectType jwtSubjectType = null;
    private AuthData lastUsedAuthData = null;
    private TextInputEditText nameEditText = null;
    private TextInputEditText subjectEditText = null;
    private LinearProgressIndicator progressBar = null;
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
        setContentView(R.layout.activity_main);
        this.nameEditText = this.findViewById(R.id.nameEditText);
        this.subjectEditText = this.findViewById(R.id.subjectEditText);
        this.progressBar = this.findViewById(R.id.progressBar);
        setSupportActionBar(this.findViewById(R.id.toolbar));
        inAppChat.activate();
        setUpPushRegIdField();
        setUpSubjectTypeSpinner();
        setUpOpenChatButton();
        setUpOpenAuthChatButton();
        setUpPersonalizationButton();
        setUpDepersonalizationButton();
    }

    @Override
    protected void onDestroy() {
        if (this.pushRegIdReceiverRegistered) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(pushRegIdReceiver);
            } catch (Throwable t) {
                MobileMessagingLogger.e(TAG, "Unable to unregister pushRegIdReceiverRegistered", t);
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (this.lastUsedAuthData != null) {
            outState.putParcelable(EXTRA_AUTH_DATA, this.lastUsedAuthData);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Parcelable parcelable = savedInstanceState.getParcelable(EXTRA_AUTH_DATA);
        if (parcelable instanceof AuthData) {
            this.lastUsedAuthData = (AuthData) parcelable;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_id) {
            setPushRegIdToClipboard(getPushRegId());
            Toast.makeText(this, R.string.toast_registration_id_copy, Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getGroupId() == R.id.languages) {
            String language = langMenuIdToLocale(item.getItemId());
            //change language of chat view
            InAppChat.getInstance(MainActivity.this).setLanguage(language, new MobileMessaging.ResultListener<String>() {
                @Override
                public void onResult(Result<String, MobileMessagingError> result) {
                    if (result.isSuccess()) {
                        MobileMessagingLogger.d(TAG, "Language changed to " + result.getData());
                    } else {
                        MobileMessagingLogger.d(TAG, "Failed to change language, reason: " + result.getError().getMessage());
                    }
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    public String langMenuIdToLocale(@IdRes int menuId) {
        if (menuId == R.id.english)
            return "en-US";
        else if (menuId == R.id.turkish)
            return "tr-TR";
        else if (menuId == R.id.korean)
            return "ko-KR";
        else if (menuId == R.id.russian)
            return "ru-RU";
        else if (menuId == R.id.chinese)
            return "zh-TW";
        else if (menuId == R.id.spanish)
            return "es-ES";
        else if (menuId == R.id.portuguese)
            return "pt-PT";
        else if (menuId == R.id.polish)
            return "pl-PL";
        else if (menuId == R.id.romanian)
            return "ro-RO";
        else if (menuId == R.id.arabic)
            return "ar-AE";
        else if (menuId == R.id.bosnian)
            return "bs-BA";
        else if (menuId == R.id.croatian)
            return "hr-HR";
        else if (menuId == R.id.greek)
            return "el-GR";
        else if (menuId == R.id.swedish)
            return "sv-SE";
        else if (menuId == R.id.thai)
            return "th-TH";
        else if (menuId == R.id.lithuanian)
            return "lt-LT";
        else if (menuId == R.id.danish)
            return "da-DK";
        else if (menuId == R.id.latvian)
            return "lv-LV";
        else if (menuId == R.id.hungarian)
            return "hu-HU";
        else if (menuId == R.id.italian)
            return "it-IT";
        else if (menuId == R.id.french)
            return "fr-FR";
        else if (menuId == R.id.slovenian)
            return "sl-SI";
        else if (menuId == R.id.ukrainian)
            return "uk-UA";
        else if (menuId == R.id.japanese)
            return "ja-JP";
        else if (menuId == R.id.german)
            return "de-DE";
        else return null;
    }

    private String getPushRegId() {
        return MobileMessaging.getInstance(this).getInstallation().getPushRegistrationId();
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

    private void setUpSubjectTypeSpinner() {
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.subjectTypeAutocompleteTextView);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.subject_types, android.R.layout.simple_dropdown_item_1line);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setOnItemClickListener((adapterView, view, position, id) -> {
            MainActivity.this.jwtSubjectType = JWTSubjectType.values()[position];
        });
    }

    private void setUpOpenChatButton() {
        Button openChat = findViewById(R.id.openChat);
        openChat.setOnClickListener((v) -> {
            inAppChat.setJwtProvider(null);
            openChat();
        });
    }

    private void setUpOpenAuthChatButton() {
        Button openAuthChat = findViewById(R.id.openAuthChat);
        openAuthChat.setOnClickListener((v) -> {
            showProgressBar();

            inAppChat.setJwtProvider(() -> {
                AuthData authData = MainActivity.this.lastUsedAuthData;
                String jwt = null;
                if (authData != null) {
                    jwt = createJwt(authData.getJwtSubjectType(), authData.getSubject(), WIDGET_ID, WIDGET_SECRET_KEY_JSON);
                }
                MobileMessagingLogger.d(TAG, "Providing JWT for " + authData + " = " + jwt);
                return jwt;
            });

            AuthData authData = createAuthData();
            if (authData != null) {
                this.lastUsedAuthData = authData;
                if (StringUtils.isBlank(WIDGET_ID) || StringUtils.isBlank(WIDGET_SECRET_KEY_JSON)) {
                    hideProgressBar();
                    Toast.makeText(MainActivity.this, "Can not create JWT, missing widgetId or widget secret key.", Toast.LENGTH_SHORT).show();
                    return;
                }
                executePersonalization(authData, new MobileMessaging.ResultListener<User>() {
                    @Override
                    public void onResult(Result<User, MobileMessagingError> result) {
                        hideProgressBar();
                        if (result.isSuccess()) {
                            Toast.makeText(MainActivity.this, "Personalization done!", Toast.LENGTH_SHORT).show();
                            openChat();
                        } else
                            Toast.makeText(MainActivity.this, "Personalization failed: " + result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                hideProgressBar();
            }
        });
    }

    private void openChat() {
        //Uncomment one of the following variants
        //1. Shows in-app chat as Fragment
        inAppChat.showInAppChatFragment(getSupportFragmentManager(), R.id.fragmentContainer);

        //2. Shows in-app chat as Activity
//        inAppChat.inAppChatView().show();
    }

    private void setUpPersonalizationButton() {
        Button personalize = findViewById(R.id.personalize);
        personalize.setOnClickListener((v) -> {
            showProgressBar();
            AuthData authData = createAuthData();
            executePersonalization(authData, new MobileMessaging.ResultListener<User>() {
                @Override
                public void onResult(Result<User, MobileMessagingError> result) {
                    hideProgressBar();
                    if (result.isSuccess())
                        Toast.makeText(MainActivity.this, "Personalization done!", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(MainActivity.this, "Personalization failed: " + result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void executePersonalization(AuthData authData, MobileMessaging.ResultListener<User> resultListener) {
        if (authData != null) {
            MobileMessaging.getInstance(this).personalize(
                    authData.getUserIdentity(),
                    authData.getUserAttributes(),
                    true,
                    resultListener
            );
        }
    }

    private AuthData createAuthData() {
        JWTSubjectType subjectType = this.jwtSubjectType;
        if (subjectType == null) {
            Toast.makeText(MainActivity.this, "Select subject type please.", Toast.LENGTH_SHORT).show();
            return null;
        }

        String subject = subjectEditText.getText() != null ? subjectEditText.getText().toString() : "";
        if (StringUtils.isBlank(subject)) {
            Toast.makeText(MainActivity.this, "Enter subject please.", Toast.LENGTH_SHORT).show();
            return null;
        }

        String name = nameEditText.getText() != null ? nameEditText.getText().toString() : "";
        if (StringUtils.isBlank(name)) {
            Toast.makeText(MainActivity.this, "Enter name please.", Toast.LENGTH_SHORT).show();
            return null;
        }

        return new AuthData(name, subjectType, subject);
    }

    private void setUpDepersonalizationButton() {
        Button depersonalize = findViewById(R.id.depersonalize);
        depersonalize.setOnClickListener((v) -> {
                    showProgressBar();
                    MobileMessaging.getInstance(this).depersonalize(
                            new MobileMessaging.ResultListener<SuccessPending>() {
                                @Override
                                public void onResult(Result<SuccessPending, MobileMessagingError> result) {
                                    hideProgressBar();
                                    if (result.isSuccess())
                                        Toast.makeText(MainActivity.this, "Depersonalization done!", Toast.LENGTH_SHORT).show();
                                    else
                                        Toast.makeText(MainActivity.this, "Depersonalization failed: " + result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }
        );
    }

    private String createJwt(JWTSubjectType subjectType, String subject, String widgetId, String secretKeyJson) {
        if (subjectType != null && subject != null && widgetId != null && secretKeyJson != null) {
            try {
                JSONObject jsonObject = new JSONObject(secretKeyJson);
                String keyId = jsonObject.getString("id");
                String keySecret = jsonObject.getString("key");
                MACSigner personalizationTokenSigner = new MACSigner(Base64.decode(keySecret, Base64.DEFAULT));
                String uuid = UUID.randomUUID().toString();

                JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                        .jwtID(uuid)
                        .subject(subject)
                        .issuer(widgetId)
                        .issueTime(new Date())
                        .expirationTime(new Date(System.currentTimeMillis() + 10000))
                        .claim("ski", keyId)
                        .claim("stp", subjectType.stp) //subjectType
                        .claim("sid", uuid) //session id
                        .build();

                SignedJWT personalizedToken = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
                personalizedToken.sign(personalizationTokenSigner);
                return personalizedToken.serialize();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Create JWT process failed!", Toast.LENGTH_SHORT).show();
            }
        }
        return null;
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.INVISIBLE);
    }

}
