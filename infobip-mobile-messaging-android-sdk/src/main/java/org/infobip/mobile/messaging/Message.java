package org.infobip.mobile.messaging;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;

/**
 * @author mstipanov
 * @since 25.03.2016.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class Message {
    private final Bundle bundle;

    public Message(Bundle bundle) {
        this.bundle = bundle;
    }

    public void copyFrom(Bundle source) {
        Message sourceMessage = new Message(source);
        setFrom(sourceMessage.getFrom());
        setMessageId(sourceMessage.getMessageId());
        setTitle(sourceMessage.getTitle());
        setBody(sourceMessage.getBody());
        setSound(sourceMessage.getSound());
        setIcon(sourceMessage.getIcon());
        setSilent(sourceMessage.isSilent());
        setData(sourceMessage.getData());
    }

    public String getMessageId() {
        return bundle.getString(Data.MESSAGE_ID.getKey(), null);
    }

    public void setMessageId(String messageId) {
        bundle.putString(Data.MESSAGE_ID.getKey(), messageId);
    }

    public String getFrom() {
        return bundle.getString(Data.FROM.getKey(), null);
    }

    public void setFrom(String from) {
        bundle.putString(Data.FROM.getKey(), from);
    }

    public String getSound() {
        return bundle.getString(Data.SOUND.getKey(), null);
    }

    public void setSound(String sound) {
        bundle.putString(Data.SOUND.getKey(), sound);
    }

    public String getIcon() {
        return bundle.getString(Data.ICON.getKey(), null);
    }

    public void setIcon(String icon) {
        bundle.putString(Data.ICON.getKey(), icon);
    }

    public String getBody() {
        return bundle.getString(Data.BODY.getKey(), null);
    }

    public void setBody(String body) {
        bundle.putString(Data.BODY.getKey(), body);
    }

    public String getTitle() {
        return bundle.getString(Data.TITLE.getKey(), null);
    }

    public void setTitle(String title) {
        bundle.putString(Data.TITLE.getKey(), title);
    }

    public Bundle getData() {
        return bundle.getBundle(Data.DATA.getKey());
    }

    public void setData(Bundle data) {
        bundle.putBundle(Data.DATA.getKey(), data);
    }

    public Bundle getBundle() {
        return bundle;
    }

    public boolean isSilent() {
        return bundle.getBoolean(Data.SILENT.getKey(), false);
    }

    public void setSilent(boolean silent) {
        bundle.putBoolean(Data.SILENT.getKey(), silent);
    }

    public static Message create(String messageId, String body) {
        Message message = new Message(new Bundle());
        message.setMessageId(messageId);
        message.setBody(body);
        return message;
    }

    private enum Data {
        MESSAGE_ID("messageId"),
        TITLE("title"),
        BODY("body"),
        SOUND("sound"),
        ICON("icon"),
        FROM("from"),
        SILENT("silent"),
        DATA("data");

        private final String key;

        Data(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
