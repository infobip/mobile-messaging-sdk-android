package org.infobip.mobile.messaging.chat.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.infobip.mobile.messaging.Message.Status;
import org.infobip.mobile.messaging.chat.ChatMessage;
import org.infobip.mobile.messaging.chat.ChatMessageStorage;
import org.infobip.mobile.messaging.chat.ChatParticipant;
import org.infobip.mobile.messaging.chat.R;
import org.infobip.mobile.messaging.interactive.NotificationAction;
import org.infobip.mobile.messaging.interactive.NotificationCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.support.annotation.VisibleForTesting.PACKAGE_PRIVATE;

public class ChatAdapter extends BaseAdapter implements ChatMessageStorage.Listener {

    private static final int MAX_ACTION_BUTTONS_IN_MESSAGE = 3;

    private final Context context;
    private final List<ChatMessage> messages = new ArrayList<>();
    private final Map<String, List<NotificationAction>> notificationActions = new HashMap<>();
    private final ActionTappedReceiver actionTappedReceiver;

    public interface ActionTappedReceiver {
        void actionTapped(ChatMessage message, NotificationAction action);
    }

    @VisibleForTesting(otherwise = PACKAGE_PRIVATE)
    public ChatAdapter(Context context, Set<NotificationCategory> notificationCategories, List<ChatMessage> existingMessages, ActionTappedReceiver actionTappedReceiver) {
        this.context = context;
        this.messages.addAll(existingMessages);
        this.actionTappedReceiver = actionTappedReceiver;

        for (NotificationCategory category : notificationCategories) {
            List<NotificationAction> actions = new ArrayList<>(category.getNotificationActions().length);
            for (NotificationAction action : category.getNotificationActions()) {
                if (!action.hasInput()) {
                    actions.add(action);
                }
            }
            actions = actions.subList(0, Math.min(actions.size(), MAX_ACTION_BUTTONS_IN_MESSAGE));
            notificationActions.put(category.getCategoryId(), actions);
        }
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public ChatMessage getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ChatMessage message = messages.get(position);
        View view = initViewHolder(convertView, message.isYours());
        fillView(view, message);
        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messages.get(position);
        return message.isYours() ? 1 : 0;
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
            notifyDataSetChanged();
        }
    }

    @Override
    public void onAllDeleted() {
        messages.clear();
        notifyDataSetChanged();
    }

    // region private methods

    private static class ViewHolder {
        private ImageView messageStatus;
        private TextView messageTextView;
        private TextView timeTextView;
        private TextView senderTextView;
        private Button[] buttons;
    }

    private String getUserName(ChatMessage message) {
        if (message == null || message.getAuthor() == null) {
            return "";
        }

        ChatParticipant author = message.getAuthor();
        return author.getUserName();
    }

    private int getPosition(String messageId) {
        for (int position = 0; position < messages.size(); position++) {
            if (messages.get(position).getId().equalsIgnoreCase(messageId)) {
                return position;
            }
        }
        return -1;
    }

    @SuppressLint("InflateParams")
    @NonNull
    private View initViewHolder(@Nullable View convertView, boolean isYours) {
        if (convertView != null) {
            return convertView;
        }

        View view;
        if (isYours) {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_chat_message_outgoing, null, false);
            ViewHolder holder = new ViewHolder();
            holder.messageTextView = view.findViewById(R.id.message_text);
            holder.timeTextView = view.findViewById(R.id.time_text);
            holder.messageStatus = view.findViewById(R.id.user_reply_status);
            view.setTag(holder);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.list_item_chat_message_incoming, null, false);
            ViewHolder holder = new ViewHolder();
            holder.messageTextView = view.findViewById(R.id.message_text);
            holder.timeTextView = view.findViewById(R.id.time_text);
            holder.senderTextView = view.findViewById(R.id.chat_company_reply_author);
            holder.buttons = new Button[]{view.findViewById(R.id.btn_action_1), view.findViewById(R.id.btn_action_2), view.findViewById(R.id.btn_action_3)};
            view.setTag(holder);
        }
        return view;
    }

    private void fillView(@NonNull View view, final ChatMessage message) {
        ViewHolder holder = (ViewHolder) view.getTag();
        if (message.isYours()) {
            holder.messageTextView.setText(message.getBody());
            holder.timeTextView.setText(DateUtils.getRelativeTimeSpanString(context, message.getCreatedAt()));

            if (message.getStatus() == Status.UNKNOWN) {
                holder.messageStatus.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_clock));
            } else if (message.getStatus() == Status.SUCCESS) {
                holder.messageStatus.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_single_tick));
            } else {
                holder.messageStatus.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_error));
            }
        } else {
            holder.messageTextView.setText(message.getBody());
            holder.timeTextView.setText(DateUtils.getRelativeTimeSpanString(context, message.getCreatedAt()));

            String userName = getUserName(message);
            if (!TextUtils.isEmpty(userName)) {
                holder.senderTextView.setText(userName);
                holder.senderTextView.setVisibility(View.VISIBLE);
            } else {
                holder.senderTextView.setVisibility(View.GONE);
            }

            List<NotificationAction> actions = notificationActions.get(message.getCategory());
            if (actions == null) actions = Collections.emptyList();
            for (int index = 0; index < holder.buttons.length; index++) {
                final NotificationAction action = index < actions.size() ? actions.get(index) : null;
                if (action != null) {
                    holder.buttons[index].setVisibility(View.VISIBLE);
                    holder.buttons[index].setText(action.getTitleResourceId());
                    holder.buttons[index].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (actionTappedReceiver != null) {
                                actionTappedReceiver.actionTapped(message, action);
                            }
                        }
                    });
                } else {
                    holder.buttons[index].setVisibility(View.GONE);
                }
            }
        }
    }

    // endregion
}
