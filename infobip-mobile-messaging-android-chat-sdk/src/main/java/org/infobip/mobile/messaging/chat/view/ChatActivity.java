package org.infobip.mobile.messaging.chat.view;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatMessageStorage;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.chat.R;
import org.infobip.mobile.messaging.chat.core.MobileChatImpl;
import org.infobip.mobile.messaging.chat.properties.MobileChatProperty;
import org.infobip.mobile.messaging.chat.properties.PropertyHelper;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.Result;
import org.infobip.mobile.messaging.mobile.common.MAsyncTask;
import org.infobip.mobile.messaging.util.StringUtils;
import org.json.JSONObject;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_EXCEPTION;
import static org.infobip.mobile.messaging.Event.API_COMMUNICATION_ERROR;
import static org.infobip.mobile.messaging.chat.ChatEvent.CHAT_MESSAGE_RECEIVED;

public class ChatActivity extends AppCompatActivity implements ChatAdapter.ActionTappedReceiver {

    public static final String MESSAGE_TEXT_EXTRA = "messageText";

    public static final String MO_ROUTE_NOT_ENABLED_ERROR_CODE = "14";
    public static final int CONNECTIVITY_ANIMATION_DELAY_MILLIS = 4000;
    public static final int CLEAR_NOTIFICATIONS_DELAY_MILLIS = 3000;

    private EditText etReply;
    private ListView messagesListView;
    private TextView connectionIndicatorView;
    private ChatAdapter listAdapter = null;
    private ChatViewSettingsResolver chatViewSettingsResolver;
    private ChatMessageStorage messageStore;
    private MobileChatImpl mobileChat;
    private boolean receiversRegistered = false;
    private boolean inForeground = false;
    private ActionMode actionMode;
    private Boolean internetConnected = null;

    private Handler handler;
    private Runnable clearNotificationsRunnable;
    private Runnable connectivityRunnable;

    private PropertyHelper propertyHelper;
    private ProgressDialog progressDialog;

    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ChatMessage message = ChatMessage.createFrom(intent);

            onMessageReceived(message);
            listAdapter.notifyDataSetChanged();

            if (inForeground) {
                clearNotificationsWithDelay();
            }
        }
    };
    private BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY)) {
                handleNotConnectedState(context);
            } else {
                handleConnectedState();
            }
        }
    };

    private void handleNotConnectedState(Context context) {
        if (messagesListView != null) {
            Snackbar noConnectivitySnackbar = Snackbar.make(messagesListView, R.string.no_internet_connection, Snackbar.LENGTH_SHORT);
            noConnectivitySnackbar.show();
        }

        if (connectionIndicatorView != null) {
            connectionIndicatorView.setBackgroundColor(ContextCompat.getColor(context, R.color.connectivityStateColor));
            connectionIndicatorView.setText(R.string.no_connection);
            connectionIndicatorView.setVisibility(View.VISIBLE);
        }
        internetConnected = false;
    }

    private void handleConnectedState() {
        if (connectionIndicatorView != null && internetConnected != null && !internetConnected) {
            Animator animator = AnimatorInflater.loadAnimator(ChatActivity.this, R.animator.connectivity_state);
            animator.setTarget(connectionIndicatorView);
            animator.start();
            connectionIndicatorView.setText(R.string.connected);
        }

        connectivityRunnable = new Runnable() {
            @Override
            public void run() {
                if (connectionIndicatorView != null) {
                    connectionIndicatorView.setVisibility(View.GONE);
                }
            }
        };
        handler.postDelayed(connectivityRunnable, CONNECTIVITY_ANIMATION_DELAY_MILLIS);
        internetConnected = true;
    }

    private void clearNotificationsWithDelay() {
        if (actionMode == null) {
            // fix for reported bug that chat notification isn't canceled when user is on the chat screen
            // we need notification displayed event in lib for this delayed behaviour to be changed
            if (clearNotificationsRunnable == null) {
                clearNotificationsRunnable = new Runnable() {
                    @Override
                    public void run() {
                        clearNotifications();
                    }
                };
            }
            handler.postDelayed(clearNotificationsRunnable, CLEAR_NOTIFICATIONS_DELAY_MILLIS);
        }
    }

    private final BroadcastReceiver errorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MobileMessagingError mobileMessagingError = null;
            if (intent.hasExtra(EXTRA_EXCEPTION)) {
                mobileMessagingError = (MobileMessagingError) intent.getSerializableExtra(EXTRA_EXCEPTION);
            }

            if (mobileMessagingError == null) {
                return;
            }

            if (MO_ROUTE_NOT_ENABLED_ERROR_CODE.equals(mobileMessagingError.getCode())) {
                showMoRouteNotEnabledDialog();
            }
        }
    };

    protected void onMessageReceived(ChatMessage message) {
        String body = message.getBody();
        if (StringUtils.isBlank(message.getCategory())) {
            Toast.makeText(this, String.format(getString(R.string.toast_message_received), body), Toast.LENGTH_LONG).show();
        }

        configureEmptyState();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
            scrollToBottom();
        }
    }

    private void showMoRouteNotEnabledDialog() {
        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(ChatActivity.this);
        alertDialogBuilderUserInput
                .setCancelable(true)
                .setMessage(getString(R.string.dialog_text_mo_route_not_enabled))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilderUserInput.create();
        alertDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        chatViewSettingsResolver = new ChatViewSettingsResolver(this);
        setTheme(chatViewSettingsResolver.getChatViewTheme());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setUpToolbar(toolbar, chatViewSettingsResolver.getChatViewTitle(), true);

        mobileChat = (MobileChatImpl) MobileChatImpl.getInstance(this);
        messageStore = mobileChat.getChatMessageStorage();
        configureEmptyState();

        propertyHelper = new PropertyHelper(this);

        etReply = findViewById(R.id.et_reply);
        ImageView ivReply = findViewById(R.id.iv_reply);
        messagesListView = findViewById(R.id.messagesListView);
        connectionIndicatorView = findViewById(R.id.connection_indicator);

        LinearLayout llEmptyState = findViewById(R.id.ll_empty_state);
        messagesListView.setEmptyView(llEmptyState);

        messagesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                messagesListView.setItemChecked(position, !messagesListView.isItemChecked(position));
                return true;
            }
        });

        messagesListView.setMultiChoiceModeListener(new ChatMultiChoiceModeListener());
        messagesListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);

        listAdapter = new ChatAdapter(this, mobileChat.chatView().getNotificationCategories(), messageStore.findAllMessages(), this);
        messagesListView.setAdapter(listAdapter);
        messageStore.registerListener(listAdapter);

        if (ivReply != null) {
            ivReply.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (TextUtils.isEmpty(getUserNameFromAuthor()) ||
                            !propertyHelper.findBoolean(MobileChatProperty.USER_NAME_DIALOG_SHOWN)) {
                        showUserNameInputDialog();
                    } else {
                        sendMessage();
                    }
                }
            });
        }

        handler = new Handler();

        registerReceivers();
        scrollToBottom();

        processSendRequestIfAny(getIntent());
    }

    @Override
    protected void onDestroy() {
        unregisterReceivers();
        messageStore.unregisterListener(listAdapter);
        if (handler != null) {
            if (clearNotificationsRunnable != null) {
                handler.removeCallbacks(clearNotificationsRunnable);
            }
            if (connectivityRunnable != null) {
                handler.removeCallbacks(connectivityRunnable);
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        inForeground = true;
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        if (actionMode == null) {
            clearNotifications();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        inForeground = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.action_mark_all_seen) {
            markAllSeen();
            return true;
        }

        if (id == R.id.action_clear_chat) {
            clearChatHistory();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public void actionTapped(ChatMessage message, NotificationAction action) {
        mobileChat.processTappedAction(message, action);
    }

    private void processSendRequestIfAny(Intent intent) {
        final String text = intent.getStringExtra(MESSAGE_TEXT_EXTRA);
        if (TextUtils.isEmpty(text)) {
            return;
        }

        etReply.setText(text);
        etReply.setSelection(text.length());
        etReply.requestFocus();

        if (TextUtils.isEmpty(getUserNameFromAuthor()) ||
                !propertyHelper.findBoolean(MobileChatProperty.USER_NAME_DIALOG_SHOWN)) {
            showUserNameInputDialog();
        } else {
            sendMessage();
        }
    }

    protected void markAllSeen() {

        new MAsyncTask<Void, Void>() {

            ProgressDialog progressDialog;

            @Override
            public void before() {
                progressDialog = ProgressDialog.show(ChatActivity.this, getString(R.string.progress_title_wait), getString(R.string.progress_processing_messages), true);
            }

            @Override
            public Void run(Void... params) {
                mobileChat.markAllMessagesRead();
                return null;
            }

            @Override
            public void after(Void aVoid) {
                progressDialog.dismiss();
            }
        }.execute();
    }

    protected void clearChatHistory() {
        if (messageStore != null) {
            messageStore.deleteAll();
        }
        configureEmptyState();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
        clearNotifications();
    }

    private void onMessageSent(ChatMessage message) {
        if (message.getStatus() == Message.Status.SUCCESS) {
            Toast.makeText(ChatActivity.this, R.string.message_sent, Toast.LENGTH_SHORT).show();
        }

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void sendMessage() {
        String text = etReply.getText().toString();
        if (text.trim().length() == 0) {
            return;
        }

        mobileChat.sendMessage(text, new JSONObject(), new MobileMessaging.ResultListener<ChatMessage>() {
            @Override
            public void onResult(Result<ChatMessage, MobileMessagingError> result) {
                if (result.isSuccess()) {
                    if (internetConnected != null && !internetConnected) {
                        showToast(R.string.sending_message_failed_no_connection);
                    }

                    onMessageSent(result.getData());
                    scrollToBottom();
                }
            }
        });

        configureEmptyState();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
            scrollToBottom();
        }

        etReply.setText("");
    }

    private void scrollToBottom() {
        if (messagesListView == null) {
            return;
        }

        messagesListView.post(new Runnable() {
            @Override
            public void run() {
                if (listAdapter != null && !listAdapter.isEmpty()) {
                    messagesListView.setSelection(listAdapter.getCount() - 1);
                }
            }
        });
    }

    protected void registerReceivers() {
        if (!receiversRegistered) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.registerReceiver(errorReceiver, new IntentFilter(API_COMMUNICATION_ERROR.getKey()));
            localBroadcastManager.registerReceiver(messageReceiver, new IntentFilter(CHAT_MESSAGE_RECEIVED.getKey()));
            registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            receiversRegistered = true;
        }
    }

    protected void unregisterReceivers() {
        if (receiversRegistered) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.unregisterReceiver(errorReceiver);
            localBroadcastManager.unregisterReceiver(messageReceiver);
            unregisterReceiver(connectivityReceiver);
            receiversRegistered = false;
        }
    }

    protected void clearNotifications() {
        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) notificationManager.cancelAll();
    }

    private void showUserNameInputDialog() {
        User user = MobileMessagingCore.getInstance(this).getUser();
        if (user != null) {
            String firstName = user.getFirstName();
            String lastName = user.getLastName();

            if (StringUtils.isNotBlank(firstName) || StringUtils.isNotBlank(lastName)) {
                createChatParticipantAndSendMessage(firstName, lastName);
                return;
            }
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        @SuppressLint("InflateParams")
        View mView = inflater.inflate(R.layout.username_dialog, null);
        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(this);
        alertDialogBuilderUserInput.setView(mView);

        final EditText etUserName = mView.findViewById(R.id.etUserName);
        etUserName.setText(getUserNameFromAuthor());
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogBox, int id) {
                        dialogBox.dismiss();
                        showProgressDialog(getString(R.string.progress_title_wait));

                        String userName = etUserName.getText().toString();

                        createChatParticipantAndSendMessage(userName, null);
                    }
                });

        AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
        alertDialogAndroid.show();
    }

    void createChatParticipantAndSendMessage(String firstName, String lastName) {
        ChatParticipant chatParticipant = mobileChat.getUserInfo();
        chatParticipant.setFirstName(firstName);
        if (StringUtils.isNotBlank(lastName)) {
            chatParticipant.setLastName(lastName);
        }
        chatParticipant.setCustomData(new JSONObject());

        mobileChat.setUserInfo(chatParticipant, new MobileMessaging.ResultListener<ChatParticipant>() {
            @Override
            public void onResult(Result<ChatParticipant, MobileMessagingError> result) {
                if (!result.isSuccess()) {
                    hideProgressDialog();
                    Toast.makeText(ChatActivity.this, result.getError().getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                propertyHelper.saveBoolean(MobileChatProperty.USER_NAME_DIALOG_SHOWN, true);
                sendMessage();
                hideProgressDialog();
            }
        });
    }

    String getUserNameFromAuthor() {
        ChatParticipant author = mobileChat.getUserInfo();
        return author.getUserName();
    }

    private class ChatMultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {
        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            final int checkedCount = messagesListView.getCheckedItemCount();

            mode.setTitle(String.format(getString(R.string.chat_selected), checkedCount));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            ChatActivity.this.getMenuInflater().inflate(R.menu.menu_chat_item, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.action_delete) {
                for (int i = 0; i < messagesListView.getCount(); i++) {
                    if (messagesListView.isItemChecked(i)) {
                        messageStore.delete(listAdapter.getItem(i).getId());
                    }
                }
            }
            actionMode.finish();
            configureEmptyState();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            listAdapter.notifyDataSetChanged();
            clearNotifications();
        }
    }

    protected void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    protected void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    protected void showToast(int resId) {
        showToast(getString(resId));
    }

    protected void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    protected void setUpToolbar(Toolbar toolbar, String title, boolean displayHomeAsUpEnabled) {
        if (toolbar == null) return;

        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setTitle("");

            TypedValue colorValue = new TypedValue();
            getTheme().resolveAttribute(R.attr.colorPrimary, colorValue, true);
            supportActionBar.setBackgroundDrawable(new ColorDrawable(colorValue.data));

            if (title != null) {
                setToolbarTitle(title);
            }

            if (displayHomeAsUpEnabled) {
                supportActionBar.setDisplayHomeAsUpEnabled(true);

                // set back arrow color to white
                Drawable drawable = toolbar.getNavigationIcon();
                if (drawable != null) {
                    drawable.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_ATOP);
                }
            } else {
                toolbar.setNavigationIcon(R.drawable.ic_menu_white);
            }
        }
    }

    protected void setToolbarTitle(String title) {
        TextView tvTitle = findViewById(R.id.tv_toolbar_title);
        if (tvTitle == null) {
            return;
        }
        tvTitle.setText(title);
    }

    private void configureEmptyState() {
        TextView tv = findViewById(R.id.tv_empty_state);
        RelativeLayout rl = findViewById(R.id.rl_chat_messages);
        if (messageStore.countAllMessages() == 0) {
            tv.setVisibility(View.VISIBLE);
            tv.setText(Html.fromHtml(chatViewSettingsResolver.getChatViewEmptyStateText()));
            tv.setClickable(true);
            tv.setMovementMethod(LinkMovementMethod.getInstance());
            rl.setVisibility(View.GONE);
        } else {
            tv.setVisibility(View.GONE);
            rl.setVisibility(View.VISIBLE);
        }
    }
}