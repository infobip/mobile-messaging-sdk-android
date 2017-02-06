package org.infobip.mobile.messaging;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.dal.bundle.BundleMapper;
import org.infobip.mobile.messaging.geo.Geo;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Message class
 *
 * @author sslavin
 * @since 05/09/16.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Message implements Comparable<Message> {

    private String messageId;
    private String title;
    private String body;
    private String sound;
    private boolean vibrate;
    private String icon;
    private boolean silent;
    private String category;
    private String from;
    private long receivedTimestamp;
    private long seenTimestamp;
    private JSONObject customPayload;
    private Geo geo;

    public enum Status {
        SUCCESS,
        ERROR,
        UNKNOWN
    }

    private String destination;
    private Status status;
    private String statusMessage;

    public static Message createFrom(Bundle bundle) {
        return BundleMapper.messageFromBundle(bundle);
    }

    public static List<Message> createFrom(ArrayList<Bundle> bundles) {
        return BundleMapper.messagesFromBundles(bundles);
    }

    public Message(String messageId, String title, String body, String sound,
                   boolean vibrate, String icon, boolean silent, String category,
                   String from, long receivedTimestamp, long seenTimestamp,
                   JSONObject customPayload, Geo geo,
                   String destination, Status status, String statusMessage) {
        this.messageId = messageId;
        this.title = title;
        this.body = body;
        this.sound = sound;
        this.vibrate = vibrate;
        this.icon = icon;
        this.silent = silent;
        this.category = category;
        this.from = from;
        this.receivedTimestamp = receivedTimestamp;
        this.seenTimestamp = seenTimestamp;
        this.customPayload = customPayload;
        this.geo = geo;
        this.destination = destination;
        this.status = status;
        this.statusMessage = statusMessage;
    }

    public Message() {
        this.messageId = UUID.randomUUID().toString();
        this.receivedTimestamp = System.currentTimeMillis();
        this.status = Status.UNKNOWN;
    }

    @Override
    public int compareTo(@NonNull Message another) {
        return (int) Math.signum(another.receivedTimestamp - receivedTimestamp);
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public boolean isDefaultSound() {
        return "default".equals(this.sound);
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public long getReceivedTimestamp() {
        return receivedTimestamp;
    }

    public void setReceivedTimestamp(long receivedTimestamp) {
        this.receivedTimestamp = receivedTimestamp;
    }

    public long getSeenTimestamp() {
        return seenTimestamp;
    }

    public void setSeenTimestamp(long seenTimestamp) {
        this.seenTimestamp = seenTimestamp;
    }

    public JSONObject getCustomPayload() {
        return customPayload;
    }

    public void setCustomPayload(JSONObject customPayload) {
        this.customPayload = customPayload;
    }

    public Geo getGeo() {
        return geo;
    }

    public void setGeo(Geo geo) {
        this.geo = geo;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}