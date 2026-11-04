/*
 * InboxActivity.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.tabs.TabLayout;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.inbox.Inbox;
import org.infobip.mobile.messaging.inbox.InboxMessage;
import org.infobip.mobile.messaging.inbox.MobileInbox;
import org.infobip.mobile.messaging.inbox.MobileInboxFilterOptions;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.util.SHA256;

import static org.infobip.mobile.messaging.util.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InboxActivity extends AppCompatActivity implements CustomAdapter.OnMessageListener,
        InboxDeleteResultListener.FailureListener {

    private static final int PAGE_SIZE = 20;
    private static final String DELETED_IDS = "deletedInboxMessageIds";
    private static final String DELETED_APP = "deletedInboxApplicationCodeHash";
    private static final String DELETED_USER = "deletedInboxExternalUserId";

    private SwipeRefreshLayout swipeLayout;
    private TextView counts;
    private Button btnBack;
    private Button btnDeleteSelected;
    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected CustomAdapter mAdapter;

    private Inbox inbox;
    private final List<InboxMessage> inboxMessages = new ArrayList<>();
    private InboxDeletionStore.UserState deletions;
    private String applicationCodeHash;
    private boolean observingDeletions;
    private final Runnable deletionObserver = () -> applyRecordedDeletions(true);
    private MobileInbox mobileInbox;
    private String externalUserId;
    private String topic;
    private List<String> topics;
    private final InboxRequestState requests = new InboxRequestState();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        mRecyclerView = findViewById(R.id.recyclerView);
        counts = findViewById(R.id.tv_counts);
        swipeLayout = findViewById(R.id.swipeContainer);
        swipeLayout.setOnRefreshListener(() -> fetchPage(true));

        btnBack = findViewById(R.id.button_second);
        btnBack.setOnClickListener(onBackPressedListener());
        btnDeleteSelected = findViewById(R.id.btn_delete_selected);
        btnDeleteSelected.setOnClickListener(view -> deleteSelectedMessages());

        mobileInbox = MobileInbox.getInstance(this);
        Intent intent = getIntent();
        if (intent != null) {
            externalUserId = intent.getStringExtra(Constants.BUNDLE_KEY_EXTERNAL_USER_ID);
        }
        applicationCodeHash = currentApplicationCodeHash();
        if (isBlank(applicationCodeHash) || isBlank(externalUserId)) {
            finish();
            return;
        }
        deletions = InboxDeletionStore.SHARED.forUser(applicationCodeHash, externalUserId);
        if (savedInstanceState != null &&
                applicationCodeHash.equals(savedInstanceState.getString(DELETED_APP)) &&
                externalUserId.equals(savedInstanceState.getString(DELETED_USER))) {
            List<String> deletedIds = savedInstanceState.getStringArrayList(DELETED_IDS);
            if (deletedIds != null) {
                deletions.recordDeleted(deletedIds);
            }
        }
        // A cached response may be a later page or use a different topic filter.
        // Always start with a fresh All page rather than restoring that response.
        counts.setText(R.string.inbox_empty);

        mAdapter = new CustomAdapter(inboxMessages, this);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);
        addLoadNextPageListener();
        addSwipeToDelete();

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.addTab(tabs.newTab().setText(R.string.tb_all));
        tabs.addTab(tabs.newTab().setText(R.string.tb_promo));
        tabs.addTab(tabs.newTab().setText(R.string.tb_notifications));
        tabs.addTab(tabs.newTab().setText(R.string.tb_offers));
        tabs.addTab(tabs.newTab().setText(R.string.tb_promotions_notifications));
        tabs.addOnTabSelectedListener(tabSelectedListener());
        fetchPage(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isCurrentInbox()) {
            finish();
            return;
        }
        observingDeletions = true;
        deletions.addListener(deletionObserver);
        // Reconcile deletes completed while this screen was stopped, before it became visible.
        applyRecordedDeletions(false);
    }

    @Override
    protected void onStop() {
        stopObservingDeletions();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (deletions != null) {
            outState.putStringArrayList(DELETED_IDS, new ArrayList<>(deletions.snapshot()));
            outState.putString(DELETED_APP, applicationCodeHash);
            outState.putString(DELETED_USER, externalUserId);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        stopObservingDeletions();
        requests.reset();
        super.onDestroy();
    }

    private void stopObservingDeletions() {
        observingDeletions = false;
        if (deletions != null) {
            deletions.removeListener(deletionObserver);
        }
    }

    private String currentApplicationCodeHash() {
        String applicationCode = MobileMessagingCore.getApplicationCode(getApplicationContext());
        return isBlank(applicationCode) ? null : SHA256.calc(applicationCode);
    }

    private boolean isCurrentInbox() {
        User user = MobileMessaging.getInstance(this).getUser();
        return deletions != null && applicationCodeHash.equals(currentApplicationCodeHash()) &&
                user != null && externalUserId.equals(user.getExternalUserId());
    }

    private boolean canUpdateInboxUi() {
        return !isDestroyed() && !isFinishing() && isCurrentInbox();
    }

    @Override
    public void onMessageClick(int position) {
        if (!canUpdateInboxUi()) {
            return;
        }
        InboxMessage message = inboxMessages.get(position);
        if (message.isSeen()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setMessage(R.string.mark_as_seen)
                .setPositiveButton("YES", (dialog, which) -> {
                    if (!canUpdateInboxUi()) {
                        return;
                    }
                    message.setSeen();
                    mobileInbox.setSeen(externalUserId, new String[]{message.getMessageId()}, new MobileMessaging.ResultListener<String[]>() {
                        @Override
                        public void onResult(Result<String[], MobileMessagingError> result) {
                            if (!canUpdateInboxUi()) {
                                return;
                            }
                            if (result.isSuccess()) {
                                fetchCounters();
                                for (int i = 0; i < inboxMessages.size(); i++) {
                                    if (message.getMessageId().equals(inboxMessages.get(i).getMessageId())) {
                                        inboxMessages.get(i).setSeen();
                                        mAdapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                            }
                        }
                    });
                })
                .setNegativeButton("NO", (dialog, which) -> {})
                .show();
    }

    @Override
    public void onMessageLongClick(int position) {
        if (!canUpdateInboxUi()) {
            return;
        }
        mAdapter.toggleSelection(inboxMessages.get(position).getMessageId());
        updateDeleteSelectedButton();
    }

    private void fetchPage(boolean startOver) {
        if (!canUpdateInboxUi()) {
            return;
        }
        final InboxRequestState.Request request = requests.beginPage(startOver);
        if (request == null) {
            return;
        }
        if (startOver) {
            mAdapter.clearSelection();
            updateDeleteSelectedButton();
        }
        MobileInboxFilterOptions options = createFilterOptions(request.cursor);
        swipeLayout.setRefreshing(true);

        mobileInbox.fetchInbox(externalUserId, options, new MobileMessaging.ResultListener<Inbox>() {
            @Override
            public void onResult(Result<Inbox, MobileMessagingError> result) {
                if (!canUpdateInboxUi()) {
                    return;
                }
                if (!result.isSuccess() || result.getData() == null) {
                    if (requests.failPage(request)) {
                        swipeLayout.setRefreshing(false);
                        Toast.makeText(InboxActivity.this, R.string.cannot_update_inbox, Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                Inbox page = result.getData();
                if (!requests.completePage(request, page.getNextCursor())) {
                    return;
                }
                swipeLayout.setRefreshing(false);
                updateCounters(request, page);
                if (request.startOver) {
                    inboxMessages.clear();
                }
                appendVisibleMessages(page.getMessages());
                mAdapter.notifyDataSetChanged();
                mRecyclerView.post(() -> loadNextPageIfNeeded());
            }
        });
    }

    private MobileInboxFilterOptions createFilterOptions(String cursor) {
        if (topics != null) {
            // Two-topic filtering is performed client-side and intentionally has no cursor.
            return new MobileInboxFilterOptions(null, null, topics, PAGE_SIZE);
        }
        return new MobileInboxFilterOptions(null, null, topic, PAGE_SIZE, cursor);
    }

    private void appendVisibleMessages(List<InboxMessage> messages) {
        if (messages == null) {
            return;
        }
        Set<String> loadedIds = new HashSet<>();
        for (InboxMessage loaded : inboxMessages) {
            loadedIds.add(loaded.getMessageId());
        }
        Set<String> deletedMessageIds = deletions.snapshot();
        for (InboxMessage message : messages) {
            if (!deletedMessageIds.contains(message.getMessageId()) && loadedIds.add(message.getMessageId())) {
                inboxMessages.add(message);
            }
        }
    }

    private void addLoadNextPageListener() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy >= 0) {
                    loadNextPageIfNeeded();
                }
            }
        });
        mRecyclerView.addOnLayoutChangeListener((view, left, top, right, bottom,
                                                oldLeft, oldTop, oldRight, oldBottom) ->
                mRecyclerView.post(() -> loadNextPageIfNeeded()));
    }

    private void loadNextPageIfNeeded() {
        if (!canUpdateInboxUi() || mRecyclerView.isLayoutRequested()) {
            return;
        }
        LinearLayoutManager manager = (LinearLayoutManager) mLayoutManager;
        if (requests.shouldLoadNextPage(mRecyclerView.canScrollVertically(1),
                manager.findLastVisibleItemPosition(), inboxMessages.size())) {
            fetchPage(false);
        }
    }

    private void addSwipeToDelete() {
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    deleteMessage(inboxMessages.get(position));
                }
            }
        });
        helper.attachToRecyclerView(mRecyclerView);
    }

    private void deleteSelectedMessages() {
        if (!canUpdateInboxUi()) {
            return;
        }
        Set<String> selectedIds = new HashSet<>(mAdapter.getSelectedMessageIds());
        if (selectedIds.isEmpty()) {
            return;
        }
        String[] messageIds = selectedIds.toArray(new String[0]);
        mobileInbox.deleteMessages(externalUserId, messageIds,
                new InboxDeleteResultListener(deletions, selectedIds, this));
    }

    private void updateDeleteSelectedButton() {
        int selectedCount = mAdapter.getSelectedMessageIds().size();
        btnDeleteSelected.setEnabled(selectedCount > 0);
        btnDeleteSelected.setText("Delete selected (" + selectedCount + ")");
    }

    private void deleteMessage(InboxMessage message) {
        if (!canUpdateInboxUi()) {
            return;
        }
        mobileInbox.deleteMessage(externalUserId, message.getMessageId(),
                new InboxDeleteResultListener(deletions, Collections.singleton(message.getMessageId()), this));
    }

    @Override
    public void onInboxDeleteFailed() {
        if (!observingDeletions || !canUpdateInboxUi()) {
            return;
        }
        mAdapter.notifyDataSetChanged();
        Toast.makeText(this, R.string.cannot_update_inbox, Toast.LENGTH_SHORT).show();
    }

    private void applyRecordedDeletions(boolean refreshCounters) {
        if (!observingDeletions || !canUpdateInboxUi()) {
            return;
        }
        Set<String> deletedIds = deletions.snapshot();
        inboxMessages.removeIf(message -> deletedIds.contains(message.getMessageId()));
        mAdapter.removeSelection(deletedIds);
        updateDeleteSelectedButton();
        if (refreshCounters || inbox != null) {
            fetchCounters();
        }
        mRecyclerView.post(() -> loadNextPageIfNeeded());
    }

    private void fetchCounters() {
        if (!canUpdateInboxUi()) {
            return;
        }
        final InboxRequestState.Request request = requests.beginCounters();
        mobileInbox.fetchInbox(externalUserId, createFilterOptions(null), new MobileMessaging.ResultListener<Inbox>() {
            @Override
            public void onResult(Result<Inbox, MobileMessagingError> result) {
                if (result.isSuccess() && result.getData() != null) {
                    updateCounters(request, result.getData());
                }
            }
        });
    }

    private void updateCounters(InboxRequestState.Request request, Inbox response) {
        if (!canUpdateInboxUi() || !requests.acceptsCounters(request)) {
            return;
        }
        if (inbox == null) {
            inbox = new Inbox();
        }
        // Counter requests must never replace the displayed messages or pagination cursor.
        inbox.setCountTotal(response.getCountTotal());
        inbox.setCountUnread(response.getCountUnread());
        inbox.setCountTotalFiltered(response.getCountTotalFiltered());
        inbox.setCountUnreadFiltered(response.getCountUnreadFiltered());
        updateCounterText();
    }

    private void updateCounterText() {
        if (inbox == null) {
            counts.setText(R.string.inbox_empty);
            return;
        }
        String countsText = String.format("%s messages total, %s unread total\n%s messages total filtered, %s unread filtered",
                inbox.getCountTotal(), inbox.getCountUnread(),
                inbox.getCountTotalFiltered() != null ? inbox.getCountTotalFiltered() : inbox.getCountTotal(),
                inbox.getCountUnreadFiltered() != null ? inbox.getCountUnreadFiltered() : inbox.getCountUnread());
        counts.setText(countsText);
    }

    private TabLayout.OnTabSelectedListener tabSelectedListener() {
        return new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Map<Integer, String> topicNameMap = new HashMap<>();
                topicNameMap.put(0, null);
                topicNameMap.put(1, "Promotions");
                topicNameMap.put(2, "Notifications");
                topicNameMap.put(3, "Offers");
                if (tab.getPosition() == 4) {
                    topic = null;
                    topics = Arrays.asList("Promotions", "Notifications");
                } else {
                    topic = topicNameMap.get(tab.getPosition());
                    topics = null;
                }
                requests.reset();
                inboxMessages.clear();
                inbox = null;
                updateCounterText();
                fetchPage(true);
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        };
    }

    private View.OnClickListener onBackPressedListener() {
        // MainActivity refreshes its own unfiltered first page when it resumes.
        return view -> finish();
    }
}
