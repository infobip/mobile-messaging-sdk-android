package org.infobip.mobile.messaging.demo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.api.shaded.google.gson.GsonBuilder;
import org.infobip.mobile.messaging.storage.MessageStore;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<Message> messages; // header titles
    private OnMessageExpandedListener onMessageExpandedListener;

    interface OnMessageExpandedListener {
        void onMessageExpanded(Message message);
    }

    ExpandableListAdapter(Context context, OnMessageExpandedListener onMessageExpandedListener) {
        this.context = context;
        this.onMessageExpandedListener = onMessageExpandedListener;

        refreshDataSet();
    }

    private void refreshDataSet() {
        MessageStore messageStore = MobileMessaging.getInstance(context).getMessageStore();
        if (null == messageStore) {
            this.messages = new ArrayList<>();
            return;
        }

        this.messages = messageStore.findAll(context);
        Collections.sort(messages);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        refreshDataSet();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return this.messages.get(groupPosition).getBody();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_item, null);
        }

        TextView txtListChild = (TextView) convertView
                .findViewById(R.id.lblListItem);

        txtListChild.setText(getFullMessageText(groupPosition));
        txtListChild.setBackgroundColor(Color.LTGRAY);
        txtListChild.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Utils.saveToClipboard(context, ((TextView) v).getText().toString());
                return true;
            }
        });

        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Message getGroup(int groupPosition) {
        return this.messages.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.messages.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        Message message = getGroup(groupPosition);
        String headerTitle = message.getBody(); //TODO trim to some max char count
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.list_group, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.lblListHeader);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        onMessageExpandedListener.onMessageExpanded(this.messages.get(groupPosition));
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private String getFullMessageText(int groupPosition) {
        Message m = messages.get(groupPosition);
        if (m == null) {
            return null;
        }

        String text = "messageId: " + m.getMessageId();
        text += "\ntitle: " + m.getTitle();
        text += "\nbody: " + m.getBody();
        text += "\nsound: " + m.getSound();
        text += "\nvibrate: " + m.isVibrate();
        text += "\nicon: " + m.getIcon();
        text += "\nsilent: " + m.isSilent();
        text += "\ncategory: " + m.getCategory();
        text += "\nfrom: " + m.getFrom();
        text += "\nreceivedTimestamp: " + m.getReceivedTimestamp();
        text += "\nseenTimestamp: " + m.getSeenTimestamp();
        if (m.getGeo() != null) {
            text += "\ngeo: " + new GsonBuilder().setPrettyPrinting().create().toJson(m.getGeo());
        }
        if (m.getCustomPayload() != null) {
            try {
                text += "\ncustomPayload: " + m.getCustomPayload().toString(1);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        text += "\ndestination: " + m.getDestination();
        text += "\nstatus: " + m.getStatus();
        text += "\nstatusMessage: " + m.getStatusMessage();

        return text;
    }
}