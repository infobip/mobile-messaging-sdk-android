package org.infobip.mobile.messaging.demo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatMessageStorage;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author sslavin
 * @since 14/11/2017.
 */

public class ChatMessagesAdapter extends Adapter<ChatMessagesAdapter.ViewHolder> implements ChatMessageStorage.Listener {

    private final Context context;
    private final List<ChatMessage> messages = new LinkedList<>();

    ChatMessagesAdapter(@NonNull Context context, @NonNull List<ChatMessage> initialMessages) {
        this.context = context;
        messages.addAll(initialMessages);
        Collections.sort(messages);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    @SuppressLint("InflateParams")
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int isYours) {
        ViewHolder holder;
        View v = LayoutInflater.from(context)
                .inflate(isYours == 1 ? R.layout.list_item_message_outgoing : R.layout.list_item_message_incoming,
                        null, false);
        holder = new ViewHolder(v);
        holder.tvMessageText = v.findViewById(R.id.tv_message_text);
        holder.tvReceivedTime = v.findViewById(R.id.tv_timestamp);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        ChatMessage message = messages.get(position);
        viewHolder.tvMessageText.setText(message.getBody());
        viewHolder.tvReceivedTime.setText(DateUtils.getRelativeTimeSpanString(context, message.getReceivedAt()));
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isYours() ? 1 : 0;
    }

    @Override
    public void onNew(ChatMessage message) {
        messages.add(message);
        Collections.sort(messages);
        notifyDataSetChanged();
    }

    @Override
    public void onUpdated(ChatMessage message) {
        int position = getPosition(message.getId());
        if (position >= 0) {
            messages.set(position, message);
        } else {
            messages.add(message);
        }
        Collections.sort(messages);
        notifyDataSetChanged();
    }

    @Override
    public void onDeleted(String messageId) {
        int position = getPosition(messageId);
        if (position >= 0) {
            messages.remove(position);
        }
    }

    @Override
    public void onAllDeleted() {
        messages.clear();
        notifyDataSetChanged();
    }

    // region private methods

    private int getPosition(String messageId) {
        for (int postion = 0; postion < messages.size(); postion++) {
            if (messages.get(postion).getId().equalsIgnoreCase(messageId)) {
                return postion;
            }
        }
        return -1;
    }

    @SuppressWarnings("WeakerAccess")
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText;
        TextView tvReceivedTime;

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    // endregion
}