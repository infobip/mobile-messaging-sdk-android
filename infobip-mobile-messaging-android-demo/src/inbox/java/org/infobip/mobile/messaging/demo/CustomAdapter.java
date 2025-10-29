/*
 * CustomAdapter.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.infobip.mobile.messaging.inbox.InboxMessage;

import java.util.List;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private List<InboxMessage> messageTexts;
    private OnMessageListener onMessageListener;

    public CustomAdapter(List<InboxMessage> inboxMessages, OnMessageListener onMessageListener) {
        this.messageTexts = inboxMessages;
        this.onMessageListener = onMessageListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView messageText;
        private final TextView messageTopic;

        OnMessageListener onMessageListener;

        public ViewHolder(View view, OnMessageListener onMessageListener) {
            super(view);
            this.onMessageListener = onMessageListener;
            view.setOnClickListener(this);

            messageText = view.findViewById(R.id.tv_message_details);
            messageTopic = view.findViewById(R.id.tv_message_topic);
        }

        @Override
        public void onClick(View view) {
            onMessageListener.onMessageClick(getAdapterPosition());
        }

        public TextView getMessageTextView() {
            return messageText;
        }

        public TextView getMessageTopicView() {
            return messageTopic;
        }
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.message_details, parent, false);

        return new ViewHolder(v, onMessageListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        InboxMessage message = messageTexts.get(position);
        String text = "Text: " + message.getBody();
        String topic = "Topic: " + message.getTopic();

        holder.getMessageTextView().setText(text);
        holder.getMessageTopicView().setText(topic);
        holder.getMessageTextView()
                .setTypeface(null, message.isSeen() ? Typeface.NORMAL : Typeface.BOLD);
    }

    @Override
    public int getItemCount() {
        return messageTexts.size();
    }

    public interface OnMessageListener {
        void onMessageClick(int position);
    }
}