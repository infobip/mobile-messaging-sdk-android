/*
 * CustomAdapter.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.demo;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.infobip.mobile.messaging.inbox.InboxMessage;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
    private final List<InboxMessage> messageTexts;
    private final OnMessageListener onMessageListener;
    private final Set<String> selectedMessageIds = new HashSet<>();

    public CustomAdapter(List<InboxMessage> inboxMessages, OnMessageListener onMessageListener) {
        this.messageTexts = inboxMessages;
        this.onMessageListener = onMessageListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        private final TextView messageText;
        private final TextView messageTopic;
        private final OnMessageListener onMessageListener;

        public ViewHolder(View view, OnMessageListener onMessageListener) {
            super(view);
            this.onMessageListener = onMessageListener;
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            messageText = view.findViewById(R.id.tv_message_details);
            messageTopic = view.findViewById(R.id.tv_message_topic);
        }

        @Override
        public void onClick(View view) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                onMessageListener.onMessageClick(position);
            }
        }

        @Override
        public boolean onLongClick(View view) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                onMessageListener.onMessageLongClick(position);
                return true;
            }
            return false;
        }
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_details, parent, false);
        return new ViewHolder(view, onMessageListener);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        InboxMessage message = messageTexts.get(position);
        holder.messageText.setText("Text: " + message.getBody());
        holder.messageTopic.setText("Topic: " + message.getTopic());
        holder.messageText.setTypeface(null, message.isSeen() ? Typeface.NORMAL : Typeface.BOLD);
        holder.itemView.setBackgroundColor(selectedMessageIds.contains(message.getMessageId())
                ? Color.LTGRAY : Color.TRANSPARENT);
    }

    @Override
    public int getItemCount() {
        return messageTexts.size();
    }

    public void toggleSelection(String messageId) {
        if (!selectedMessageIds.add(messageId)) {
            selectedMessageIds.remove(messageId);
        }
        notifyDataSetChanged();
    }

    public Set<String> getSelectedMessageIds() {
        return Collections.unmodifiableSet(selectedMessageIds);
    }

    public void removeSelection(Set<String> messageIds) {
        selectedMessageIds.removeAll(messageIds);
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedMessageIds.clear();
        notifyDataSetChanged();
    }

    public interface OnMessageListener {
        void onMessageClick(int position);
        void onMessageLongClick(int position);
    }
}
