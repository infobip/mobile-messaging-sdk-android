/*
 * InboxActivity.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.tabs.TabLayout;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.inbox.Inbox;
import org.infobip.mobile.messaging.inbox.InboxBundleMapper;
import org.infobip.mobile.messaging.inbox.InboxMessage;
import org.infobip.mobile.messaging.inbox.MobileInbox;
import org.infobip.mobile.messaging.inbox.MobileInboxFilterOptions;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InboxActivity extends AppCompatActivity implements CustomAdapter.OnMessageListener {

    private SwipeRefreshLayout swipeLayout;
    private TextView counts;
    private Button btnBack;
    protected RecyclerView mRecyclerView;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected CustomAdapter mAdapter;

    private Inbox inbox;
    private List<InboxMessage> inboxMessages;
    private MobileInbox mobileInbox;
    private String externalUserId;
    private String topic;
    private List<String> topics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        mRecyclerView = findViewById(R.id.recyclerView);
        counts = findViewById(R.id.tv_counts);
        swipeLayout = findViewById(R.id.swipeContainer);
        swipeLayout.setOnRefreshListener(fetchInboxOnSwipeListener());

        btnBack = findViewById(R.id.button_second);
        btnBack.setOnClickListener(onBackPressedListener());

        TabLayout tabs = findViewById(R.id.tabs);

        mobileInbox = MobileInbox.getInstance(this);
        topic = null;
        topics = null;

        Intent intent = getIntent();
        if (intent != null) {
            externalUserId = intent.getStringExtra(Constants.BUNDLE_KEY_EXTERNAL_USER_ID);
            inbox = Inbox.createFrom(intent.getBundleExtra(Constants.BUNDLE_KEY_DEMO_INBOX));
        }

        inboxMessages = new ArrayList<>();
        if (inbox != null && inbox.getCountTotal() > 0) {
            inboxMessages.addAll(inbox.getMessages());
            updateCounterText();
        } else {
            counts.setText(R.string.inbox_empty);
        }

        mAdapter = new CustomAdapter(inboxMessages, this);

        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(0);

        tabs.addTab(tabs.newTab().setText(R.string.tb_all));
        tabs.addTab(tabs.newTab().setText(R.string.tb_promo));
        tabs.addTab(tabs.newTab().setText(R.string.tb_notifications));
        tabs.addTab(tabs.newTab().setText(R.string.tb_offers));
        tabs.addTab(tabs.newTab().setText(R.string.tb_promotions_notifications));
        tabs.addOnTabSelectedListener(tabSelectedListener());
    }

    @Override
    public void onMessageClick(int i) {
        String messageId = inboxMessages.get(i).getMessageId();
        if (inboxMessages.get(i).isSeen()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(R.string.mark_as_seen)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        inboxMessages.get(i).setSeen();
                        mobileInbox.setSeen(externalUserId, new String[]{messageId}, new MobileMessaging.ResultListener<String[]>() {
                            @SuppressLint("NotifyDataSetChanged")
                            @Override
                            public void onResult(Result<String[], MobileMessagingError> result) {
                                if (result.isSuccess()) {
                                    updateCounterText();
                                    mAdapter.notifyDataSetChanged();
                                }
                            }
                        });
                    }
                }).setNegativeButton("NO", (dialog, which) -> {});
        builder.show();
    }

    private void updateCounterText() {
        String countsText = String.format("%s messages total, %s unread total\n%s messages total filtered, %s unread filtered",
                inbox.getCountTotal(), inbox.getCountUnread(), inbox.getCountTotalFiltered() != null ? inbox.getCountTotalFiltered() : inbox.getCountTotal(), inbox.getCountUnreadFiltered() != null ? inbox.getCountUnreadFiltered() : inbox.getCountUnread()
        );
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
                if (inbox != null && inbox.getCountTotal() > 0) {
                    updateInboxList();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        };
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateInboxList() {
        inboxMessages.clear();
        for (InboxMessage message : inbox.getMessages()) {
            if (topic == null || message.getTopic().equalsIgnoreCase(topic)) {
                inboxMessages.add(message);
            }
        }
        mAdapter.notifyDataSetChanged();
    }

    private SwipeRefreshLayout.OnRefreshListener fetchInboxOnSwipeListener() {
        return () -> {
            MobileInboxFilterOptions filterOptions = topics != null ? new MobileInboxFilterOptions(null, null, topics, 5) :
                    new MobileInboxFilterOptions(null, null, topic, 5);
            mobileInbox.fetchInbox(externalUserId, filterOptions, new MobileMessaging.ResultListener<Inbox>() {
                @Override
                public void onResult(Result<Inbox, MobileMessagingError> result) {
                    if (result.isSuccess()) {
                        inbox = result.getData();
                        if (inbox.getCountTotal() > 0) {
                            updateInboxList();
                            updateCounterText();
                        } else {
                            Toast.makeText(InboxActivity.this, R.string.inbox_empty, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(InboxActivity.this, R.string.cannot_update_inbox, Toast.LENGTH_SHORT).show();
                    }
                    swipeLayout.setRefreshing(false);
                }
            });
        };
    }

    private View.OnClickListener onBackPressedListener() {
        return view -> {
            Bundle arguments = new Bundle();
            arguments.putBundle(Constants.BUNDLE_KEY_DEMO_INBOX, InboxBundleMapper.inboxToBundle(inbox));

            Intent intent = new Intent(InboxActivity.this, MainActivity.class);
            intent.putExtras(arguments);
            InboxActivity.this.startActivity(intent);
        };
    }
}