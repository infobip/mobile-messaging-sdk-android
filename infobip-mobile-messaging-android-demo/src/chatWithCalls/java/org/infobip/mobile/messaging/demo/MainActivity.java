package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.infobip.webrtc.ui.InfobipRtcUi;
import com.infobip.webrtc.ui.model.InCallButton;

import org.infobip.mobile.messaging.BroadcastParameter;
import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.SuccessPending;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.api.chat.WidgetInfo;
import org.infobip.mobile.messaging.chat.InAppChat;
import org.infobip.mobile.messaging.chat.core.InAppChatEvent;
import org.infobip.mobile.messaging.chat.core.InAppChatException;
import org.infobip.mobile.messaging.chat.core.JwtProvider;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetLanguage;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetMessage;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetResult;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThread;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThreads;
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView;
import org.infobip.mobile.messaging.chat.view.InAppChatErrorsHandler;
import org.infobip.mobile.messaging.chat.view.InAppChatEventsListener;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatInputViewStyle;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatTheme;
import org.infobip.mobile.messaging.chat.view.styles.InAppChatToolbarStyle;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.Arrays;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import kotlin.Unit;

public class MainActivity extends AppCompatActivity {

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
    private boolean lcRegIdReceiverRegistered = false;
    private boolean inAppChatAvailabilityReceiverRegistered = false;
    private JWTSubjectType jwtSubjectType = null;
    private AuthData lastUsedAuthData = null;
    private TextInputEditText nameEditText = null;
    private TextInputEditText subjectEditText = null;
    private LinearProgressIndicator progressBar = null;
    private Button openChatActivityButton = null;
    private Button openChatFragmentButton = null;
    private Button openChatViewButton = null;

    private final BroadcastReceiver pushRegIdReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                showPushRegId(intent.getStringExtra(BroadcastParameter.EXTRA_INFOBIP_ID));
            }
        }
    };
    private final BroadcastReceiver lcRegIdReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                showLivechatRegId(intent.getStringExtra(BroadcastParameter.EXTRA_LIVECHAT_REGISTRATION_ID));
            }
        }
    };
    private final BroadcastReceiver isInAppChatAvailableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                onChatAvailabilityUpdated(intent.getBooleanExtra(BroadcastParameter.EXTRA_IS_CHAT_AVAILABLE, inAppChat.isChatAvailable()), true);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.nameEditText = this.findViewById(R.id.nameEditText);
        this.subjectEditText = this.findViewById(R.id.subjectEditText);
        this.progressBar = this.findViewById(R.id.progressBar);
        this.openChatActivityButton = findViewById(R.id.openChatActivity);
        this.openChatFragmentButton = findViewById(R.id.openChatFragment);
        this.openChatViewButton = findViewById(R.id.openChatView);
        setSupportActionBar(this.findViewById(R.id.toolbar));
        inAppChat.activate();
        setUpPushRegIdField();
        setUpLivechatRegIdField();
        setUpSubjectTypeSpinner();
        setUpOpenChatActivityButton();
        setUpOpenChatFragmentButton();
        setUpOpenChatViewButton();
        setUpAuthButton();
        setUpWidgetApiButton();
        setUpRuntimeCustomization();
        setUpPersonalizationButton();
        setUpDepersonalizationButton();
        setUpCallsButtons();
        setUpInAppChatAvailabilityReceiver();
        onChatAvailabilityUpdated(inAppChat.isChatAvailable(), false);
    }

    @Override
    protected void onDestroy() {
        pushRegIdReceiverRegistered = !unregisterBroadcastReceiver(pushRegIdReceiverRegistered, pushRegIdReceiver);
        lcRegIdReceiverRegistered = !unregisterBroadcastReceiver(lcRegIdReceiverRegistered, lcRegIdReceiver);
        inAppChatAvailabilityReceiverRegistered = !unregisterBroadcastReceiver(inAppChatAvailabilityReceiverRegistered, isInAppChatAvailableReceiver);
        super.onDestroy();
    }

    private boolean unregisterBroadcastReceiver(Boolean isRegistered, BroadcastReceiver receiver) {
        if (isRegistered) {
            try {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
                return true;
            } catch (Throwable t) {
                MobileMessagingLogger.e("MainActivity", "Unable to unregister broadcast receiver", t);
            }
        }
        return false;
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
            saveToClipboard(getString(R.string.push_registration_id), getPushRegId());
            Toast.makeText(this, getString(R.string.push_registration_id) + " " + getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
            return true;
        }
        else if (item.getGroupId() == R.id.languages) {
            LivechatWidgetLanguage language = lcLangFromId(item.getItemId());
            if (language != null) {
                InAppChat.getInstance(MainActivity.this).setLanguage(language);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public LivechatWidgetLanguage lcLangFromId(@IdRes int menuId) {
        if (menuId == R.id.english)
            return LivechatWidgetLanguage.ENGLISH;
        else if (menuId == R.id.turkish)
            return LivechatWidgetLanguage.TURKISH;
        else if (menuId == R.id.korean)
            return LivechatWidgetLanguage.KOREAN;
        else if (menuId == R.id.russian)
            return LivechatWidgetLanguage.RUSSIAN;
        else if (menuId == R.id.chinese_traditional)
            return LivechatWidgetLanguage.CHINESE_TRADITIONAL;
        else if (menuId == R.id.chinese_simplified)
            return LivechatWidgetLanguage.CHINESE_SIMPLIFIED;
        else if (menuId == R.id.spanish)
            return LivechatWidgetLanguage.SPANISH;
        else if (menuId == R.id.portuguese)
            return LivechatWidgetLanguage.PORTUGUESE;
        else if (menuId == R.id.polish)
            return LivechatWidgetLanguage.POLISH;
        else if (menuId == R.id.romanian)
            return LivechatWidgetLanguage.ROMANIAN;
        else if (menuId == R.id.arabic)
            return LivechatWidgetLanguage.ARABIC;
        else if (menuId == R.id.bosnian)
            return LivechatWidgetLanguage.BOSNIAN;
        else if (menuId == R.id.croatian)
            return LivechatWidgetLanguage.CROATIAN;
        else if (menuId == R.id.greek)
            return LivechatWidgetLanguage.GREEK;
        else if (menuId == R.id.swedish)
            return LivechatWidgetLanguage.SWEDISH;
        else if (menuId == R.id.thai)
            return LivechatWidgetLanguage.THAI;
        else if (menuId == R.id.lithuanian)
            return LivechatWidgetLanguage.LITHUANIAN;
        else if (menuId == R.id.danish)
            return LivechatWidgetLanguage.DANISH;
        else if (menuId == R.id.latvian)
            return LivechatWidgetLanguage.LATVIAN;
        else if (menuId == R.id.hungarian)
            return LivechatWidgetLanguage.HUNGARIAN;
        else if (menuId == R.id.italian)
            return LivechatWidgetLanguage.ITALIAN;
        else if (menuId == R.id.french)
            return LivechatWidgetLanguage.FRENCH;
        else if (menuId == R.id.slovenian)
            return LivechatWidgetLanguage.SLOVENIAN;
        else if (menuId == R.id.ukrainian)
            return LivechatWidgetLanguage.UKRAINIAN;
        else if (menuId == R.id.japanese)
            return LivechatWidgetLanguage.JAPANESE;
        else if (menuId == R.id.german)
            return LivechatWidgetLanguage.GERMAN;
        else if (menuId == R.id.albanian)
            return LivechatWidgetLanguage.ALBANIAN;
        else if (menuId == R.id.serbian)
            return LivechatWidgetLanguage.SERBIAN;
        else return null;
    }

    private String getPushRegId() {
        return MobileMessaging.getInstance(this).getInstallation().getPushRegistrationId();
    }

    private void setUpPushRegIdField() {
        String pushRegId = getPushRegId();
        if (!showPushRegId(pushRegId) && !pushRegIdReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(pushRegIdReceiver, new IntentFilter(Event.REGISTRATION_CREATED.getKey()));
            this.pushRegIdReceiverRegistered = true;
        }
    }

    private void saveToClipboard(String label, String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, value);
        clipboard.setPrimaryClip(clip);
    }

    private boolean showPushRegId(String pushRegId) {
        if (StringUtils.isNotBlank(pushRegId)) {
            TextInputEditText pushRegIdEditText = findViewById(R.id.pushRegIdEditText);
            pushRegIdEditText.setText(pushRegId);
            pushRegIdEditText.setKeyListener(null);
            pushRegIdEditText.setOnClickListener(view -> {
                saveToClipboard(getString(R.string.push_registration_id), pushRegId);
            });
            return true;
        }
        return false;
    }

    private void setUpLivechatRegIdField() {
        TextInputLayout lcRegIdInputLayout = findViewById(R.id.lcRegIdInputLayout);
        lcRegIdInputLayout.setVisibility(View.VISIBLE);
        if (!lcRegIdReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(lcRegIdReceiver, new IntentFilter(InAppChatEvent.LIVECHAT_REGISTRATION_ID_UPDATED.getKey()));
            this.pushRegIdReceiverRegistered = true;
        }
    }

    private void showLivechatRegId(String lcRegId) {
        if (StringUtils.isNotBlank(lcRegId)) {
            TextInputEditText lcRegIdEditText = findViewById(R.id.lcRegIdEditText);
            lcRegIdEditText.setText(lcRegId);
            lcRegIdEditText.setKeyListener(null);
            lcRegIdEditText.setOnClickListener(view -> {
                saveToClipboard(getString(R.string.livechat_registration_id), lcRegId);
            });
        }
    }

    private void setUpSubjectTypeSpinner() {
        AutoCompleteTextView autoCompleteTextView = findViewById(R.id.subjectTypeAutocompleteTextView);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.subject_types, android.R.layout.simple_dropdown_item_1line);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.setOnItemClickListener((adapterView, view, position, id) -> {
            MainActivity.this.jwtSubjectType = JWTSubjectType.values()[position];
        });
    }

    private void setUpInAppChatAvailabilityReceiver() {
        if (!inAppChatAvailabilityReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(isInAppChatAvailableReceiver, new IntentFilter(InAppChatEvent.IN_APP_CHAT_AVAILABILITY_UPDATED.getKey()));
            this.inAppChatAvailabilityReceiverRegistered = true;
        }
    }

    private void onChatAvailabilityUpdated(boolean isAvailable, boolean showToast) {
        openChatActivityButton.setEnabled(isAvailable);
        openChatFragmentButton.setEnabled(isAvailable);
        openChatViewButton.setEnabled(isAvailable);
        if (showToast)
            Toast.makeText(this, getString(R.string.chat_availability, isAvailable), Toast.LENGTH_SHORT).show();
    }

    private InAppChatEventsListener getInAppChatEventsListener() {
        return new InAppChatEventsListener() {

            @Override
            public void onChatLanguageChanged(@NonNull LivechatWidgetResult<String> result) {
                MobileMessagingLogger.d(TAG, "On chat language changed: " + result);
            }

            @Override
            public void onChatThreadCreated(@NonNull LivechatWidgetResult<? extends LivechatWidgetMessage> result) {
                MobileMessagingLogger.d(TAG, "On chat thread created: " + result);
            }

            @Override
            public void onChatThreadListShown(@NonNull LivechatWidgetResult<Unit> result) {
                MobileMessagingLogger.d(TAG, "On chat thread list shown: " + result);
            }

            @Override
            public void onChatThreadShown(@NonNull LivechatWidgetResult<LivechatWidgetThread> result) {
                MobileMessagingLogger.d(TAG, "On chat thread shown: " + result);
            }

            @Override
            public void onChatActiveThreadReceived(@NonNull LivechatWidgetResult<LivechatWidgetThread> result) {
                MobileMessagingLogger.d(TAG, "On chat active thread received: " + result);
            }

            @Override
            public void onChatThreadsReceived(@NonNull LivechatWidgetResult<LivechatWidgetThreads> result) {
                MobileMessagingLogger.d(TAG, "On chat threads received: " + result);
            }

            @Override
            public void onChatContextualDataSent(@NonNull LivechatWidgetResult<String> result) {
                MobileMessagingLogger.d(TAG, "On chat contextual data sent: " + result);
            }

            @Override
            public void onChatSent(@NonNull LivechatWidgetResult<? extends LivechatWidgetMessage> result) {
                MobileMessagingLogger.d(TAG, "On chat sent: " + result);
            }

            @Override
            public void onChatLoadingFinished(@NonNull LivechatWidgetResult<Unit> result) {
                MobileMessagingLogger.d(TAG, "On chat loading finished: " + result);
            }

            @Override
            public void onChatRawMessageReceived(@NonNull String rawMessage) {
                MobileMessagingLogger.d(TAG, "On chat raw message received: " + rawMessage);
            }

            @Override
            public void onChatWidgetThemeChanged(@NonNull LivechatWidgetResult<String> result) {
                MobileMessagingLogger.d(TAG, "On chat widget theme changed: " + result);
            }

            @Override
            public void onChatWidgetInfoUpdated(@NonNull WidgetInfo widgetInfo) {
                MobileMessagingLogger.d(TAG, "On chat widget info updated: " + widgetInfo);
            }

            @Override
            public void onChatViewChanged(@NonNull LivechatWidgetView widgetView) {
                MobileMessagingLogger.d(TAG, "On chat view changed: " + widgetView);
            }

            @Override
            public void onChatControlsVisibilityChanged(boolean isVisible) {
                MobileMessagingLogger.d(TAG, "On chat controls visibility changed: " + isVisible);
            }

            @Override
            public void onChatConnectionResumed(@NonNull LivechatWidgetResult<Unit> result) {
                MobileMessagingLogger.d(TAG, "On chat connection resumed: " + result);
            }

            @Override
            public void onChatConnectionPaused(@NonNull LivechatWidgetResult<Unit> result) {
                MobileMessagingLogger.d(TAG, "On chat connection paused: " + result);
            }
        };
    }

    private InAppChatErrorsHandler getInAppChatErrorHandler() {
        return new InAppChatErrorsHandler() {
            @Override
            public void handlerError(@NonNull String error) {
                MobileMessagingLogger.d(TAG, "[DEPRECATED] On demo app handle error: " + error);
            }

            @Override
            public void handlerWidgetError(@NonNull String error) {
                MobileMessagingLogger.d(TAG, "[DEPRECATED] On demo app handle widget error: " + error);
            }

            @Override
            public void handlerNoInternetConnectionError(boolean hasConnection) {
                MobileMessagingLogger.d(TAG, "[DEPRECATED] On demo app handle no internet connection error: " + hasConnection);
            }

            @Override
            public boolean handleError(@NonNull InAppChatException exception) {
                MobileMessagingLogger.d(TAG, "On demo app handle exception: " + exception.getMessage());
                return true;
            }
        };
    }

    private void setUpOpenChatActivityButton() {
        openChatActivityButton.setOnClickListener((v) -> {
            //inAppChat.inAppChatScreen().setErrorHandler(getInAppChatErrorHandler());
            inAppChat.inAppChatScreen().setEventsListener(getInAppChatEventsListener());
            inAppChat.inAppChatScreen().show();
        });
    }

    private void setUpOpenChatFragmentButton() {
        CheckBox toolbarCheckbox = findViewById(R.id.toolbarCheckbox);
        CheckBox inputCheckbox = findViewById(R.id.inputCheckbox);
        openChatFragmentButton.setOnClickListener((v) -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            Fragment fragment = new InAppChatFragmentDemoFragment();
            Bundle args = new Bundle();
            args.putBoolean(InAppChatFragmentDemoFragment.ARG_WITH_TOOLBAR, toolbarCheckbox.isChecked());
            args.putBoolean(InAppChatFragmentDemoFragment.ARG_WITH_INPUT, inputCheckbox.isChecked());
            fragment.setArguments(args);
            fragmentTransaction.add(R.id.fragmentContainer, fragment, InAppChatFragmentDemoFragment.class.getSimpleName());
            fragmentTransaction.commit();
        });
    }

    private void setUpOpenChatViewButton() {
        openChatViewButton.setOnClickListener((v) -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.fragmentContainer, new InAppChatViewDemoFragment(), InAppChatViewDemoFragment.class.getSimpleName());
            fragmentTransaction.commit();
        });
    }

    private void setUpAuthButton() {
        Button authButton = findViewById(R.id.authenticate);
        authButton.setOnClickListener((v) -> {
            showProgressBar();

            JwtProvider livechatWidgetJwtProvider = callback -> {
                AuthData authData = MainActivity.this.lastUsedAuthData;
                String jwt = null;
                if (authData != null) {
                    jwt = JWTUtils.createJwt(authData.getJwtSubjectType(), authData.getSubject(), WIDGET_ID, WIDGET_SECRET_KEY_JSON);
                    if (jwt == null) {
                        Toast.makeText(MainActivity.this, "Create JWT process failed!", Toast.LENGTH_SHORT).show();
                    }
                }
                MobileMessagingLogger.d(TAG, "Providing JWT for " + authData + " = " + jwt);
                if (StringUtils.isNotBlank(jwt)) {
                    callback.onJwtReady(jwt);
                }
                else {
                    callback.onJwtError(new IllegalStateException("JWT is null, check widgetId and widget secret key."));
                }
            };
            inAppChat.setWidgetJwtProvider(livechatWidgetJwtProvider);

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
                        if (result.isSuccess())
                            Toast.makeText(MainActivity.this, "Authentication done!", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(MainActivity.this, "Authentication failed: " + result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            else {
                hideProgressBar();
            }
        });
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
                    true,
                    resultListener
            );
        }
        else {
            hideProgressBar();
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

    private void setUpCallsButtons() {
        Button enableCalls = findViewById(R.id.enableCalls);
        Button disableCalls = findViewById(R.id.disableCalls);
        InfobipRtcUi infobipRtcUi = InfobipRtcUi.getInstance(this);
        enableCalls.setOnClickListener(view -> {
            infobipRtcUi.enableInAppChatCalls(
                    () -> Toast.makeText(this, "Calls enabled!", Toast.LENGTH_SHORT).show(),
                    throwable -> {
                        Toast.makeText(this, "Cannot enable calls: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        MobileMessagingLogger.e("MainActivity", "Cannot enable calls: " + throwable.getMessage(), throwable);
                    }
            );
        });
        disableCalls.setOnClickListener(view -> {
            infobipRtcUi.disableCalls(
                    () -> Toast.makeText(this, "Calls disabled!", Toast.LENGTH_SHORT).show(),
                    throwable -> {
                        Toast.makeText(this, "Cannot disable calls: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                        MobileMessagingLogger.e("MainActivity", "Cannot disable calls: " + throwable.getMessage(), throwable);
                    }
            );
        });
        SwitchCompat customButtonChecked = findViewById(R.id.customButtonChecked);
        SwitchCompat customButtonEnabled = findViewById(R.id.customButtonEnabled);
        infobipRtcUi.setInCallButtons(
                Arrays.asList(
                        new InCallButton.Mute(() -> {
                            MobileMessagingLogger.d("MainActivity", "MUTE button pressed");
                            return Unit.INSTANCE;
                        }),
                        new InCallButton.Custom(
                                R.string.app_name, com.infobip.webrtc.ui.R.drawable.ic_calls_30,
                                null,
                                () -> {
                                    MobileMessagingLogger.d("MainActivity", "CUSTOM button pressed");
                                    return Unit.INSTANCE;
                                },
                                customButtonChecked::isChecked,
                                customButtonEnabled::isChecked
                        ),
                        new InCallButton.Video(() -> Unit.INSTANCE),
                        new InCallButton.ScreenShare(() -> Unit.INSTANCE),
                        new InCallButton.Speaker(() -> Unit.INSTANCE),
                        new InCallButton.FlipCam(() -> Unit.INSTANCE)

                )
        );
        enableCalls.setVisibility(View.VISIBLE);
        disableCalls.setVisibility(View.VISIBLE);
        customButtonChecked.setVisibility(View.VISIBLE);
        customButtonEnabled.setVisibility(View.VISIBLE);
    }

    private void setUpRuntimeCustomization() {
        findViewById(R.id.customization).setOnClickListener(v -> {
            InAppChatToolbarStyle toolbar = new InAppChatToolbarStyle(
                    Color.LTGRAY,
                    Color.LTGRAY,
                    false,
                    ResourcesCompat.getDrawable(getResources(), org.infobip.mobile.messaging.resources.R.drawable.mm_ic_button_decline, getTheme()),
                    Color.MAGENTA,
                    ResourcesCompat.getDrawable(getResources(), org.infobip.mobile.messaging.resources.R.drawable.mm_ic_button_accept, getTheme()),
                    Color.RED,
                    R.style.InAppChat_Demo_Toolbar_Title_TextAppearance,
                    Color.BLACK,
                    "Chat",
                    null,
                    true,
                    null,
                    Color.DKGRAY,
                    "#1",
                    null,
                    true
            );
            inAppChat.setTheme(
                    new InAppChatTheme(
                            toolbar,
                            toolbar,
                            new InAppChatStyle(
                                    Color.LTGRAY,
                                    Color.MAGENTA,
                                    "Offline",
                                    null,
                                    null,
                                    Color.BLACK,
                                    Color.CYAN
                            ),
                            new InAppChatInputViewStyle(
                                    org.infobip.mobile.messaging.chat.R.style.IB_Chat_Input_TextAppearance,
                                    Color.BLACK,
                                    Color.LTGRAY,
                                    "Type message",
                                    null,
                                    Color.GRAY,
                                    ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_add, getTheme()),
                                    ColorStateList.valueOf(Color.MAGENTA),
                                    null,
                                    Color.RED,
                                    ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_send, getTheme()),
                                    ColorStateList.valueOf(Color.MAGENTA),
                                    null,
                                    Color.RED,
                                    Color.GRAY,
                                    true,
                                    Color.MAGENTA
                            )
                    )
            );
            Toast.makeText(this, "Custom style applied", Toast.LENGTH_SHORT).show();
        });
    }

    private void setUpWidgetApiButton() {
        Button openWidgetApiButton = findViewById(R.id.openWidgetApi);
        openWidgetApiButton.setOnClickListener(v -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.fragmentContainer, new LivechatWidgetApiFragment(), LivechatWidgetApiFragment.class.getSimpleName());
            fragmentTransaction.commit();
        });
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.INVISIBLE);
    }

}
