package org.infobip.mobile.messaging;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.util.StringUtils;
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

    public static final String MESSAGE_TYPE_CHAT = "chat";
    public static final String MESSAGE_TYPE_GEO = "geo";
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
    private long sentTimestamp;
    private long inAppExpiryTimestamp;
    private JSONObject customPayload;
    private String internalData;
    private String contentUrl;
    private String webViewUrl;
    private String browserUrl;
    private InAppStyle inAppStyle;
    private String messageType;
    private String deeplink;
    private String inAppOpenTitle;
    private String inAppDismissTitle;

    public enum Status {
        SUCCESS,
        ERROR,
        UNKNOWN
    }

    public enum InAppStyle {
        @SerializedName(value = "MODAL", alternate = "0")
        MODAL,
        @SerializedName(value = "BANNER", alternate = "1")
        BANNER
    }

    private String destination;
    private Status status;
    private String statusMessage;

    public static Message createFrom(Bundle bundle) {
        return MessageBundleMapper.messageFromBundle(bundle);
    }

    public static List<Message> createFrom(ArrayList<Bundle> bundles) {
        return MessageBundleMapper.messagesFromBundles(bundles);
    }

    public Message(String messageId, String title, String body, String sound,
                   boolean vibrate, String icon, boolean silent, String category,
                   String from, long receivedTimestamp, long seenTimestamp, long sentTimestamp,
                   JSONObject customPayload, String internalData,
                   String destination, Status status, String statusMessage, String contentUrl, InAppStyle inAppStyle,
                   long inAppExpiryTimestamp, String webViewUrl, String browserUrl, String messageType, String deeplink,
                   String inAppOpenTitle, String inAppDismissTitle) {
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
        this.sentTimestamp = sentTimestamp;
        this.customPayload = customPayload;
        this.internalData = internalData;
        this.destination = destination;
        this.status = status;
        this.statusMessage = statusMessage;
        this.contentUrl = contentUrl;
        this.inAppStyle = inAppStyle;
        this.inAppExpiryTimestamp = inAppExpiryTimestamp;
        this.webViewUrl = webViewUrl;
        this.browserUrl = browserUrl;
        this.messageType = messageType;
        this.deeplink = deeplink;
        this.inAppOpenTitle = inAppOpenTitle;
        this.inAppDismissTitle = inAppDismissTitle;
    }

    public Message() {
        this.messageId = UUID.randomUUID().toString();
        this.sentTimestamp = Time.now();
        this.receivedTimestamp = Time.now();
        this.status = Status.UNKNOWN;
    }

    @Override
    public int compareTo(@NonNull Message another) {
        if (another.sentTimestamp != 0 && sentTimestamp != 0) {
            return (int) Math.signum(another.sentTimestamp - sentTimestamp);
        }
        return (int) Math.signum(another.receivedTimestamp - receivedTimestamp);
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getInternalData() {
        return internalData;
    }

    public void setInternalData(String internalData) {
        this.internalData = internalData;
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

    public long getSentTimestamp() {
        return sentTimestamp;
    }

    public void setSentTimestamp(long sentTimestamp) {
        this.sentTimestamp = sentTimestamp;
    }

    public long getInAppExpiryTimestamp() {
        return inAppExpiryTimestamp;
    }

    public void setInAppExpiryTimestamp(long inAppExpiryTimestamp) {
        this.inAppExpiryTimestamp = inAppExpiryTimestamp;
    }

    public JSONObject getCustomPayload() {
        return customPayload;
    }

    public void setCustomPayload(JSONObject customPayload) {
        this.customPayload = customPayload;
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

    public InAppStyle getInAppStyle() {
        return inAppStyle;
    }

    public void setInAppStyle(InAppStyle inAppStyle) {
        this.inAppStyle = inAppStyle;
    }

    public String getWebViewUrl() {
        return webViewUrl;
    }

    public void setWebViewUrl(String webViewUrl) {
        this.webViewUrl = webViewUrl;
    }

    public String getBrowserUrl() {
        return browserUrl;
    }

    public void setBrowserUrl(String browserUrl) {
        this.browserUrl = browserUrl;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public boolean isChatMessage() {
        return StringUtils.isNotBlank(messageType) && MESSAGE_TYPE_CHAT.equals(messageType);
    }

    public String getDeeplink() {
        return deeplink;
    }

    public void setDeeplink(String deeplink) {
        this.deeplink = deeplink;
    }

    public String getInAppOpenTitle() {
        return inAppOpenTitle;
    }

    public void setInAppOpenTitle(String inAppOpenTitle) {
        this.inAppOpenTitle = inAppOpenTitle;
    }

    public String getInAppDismissTitle() {
        return inAppDismissTitle;
    }

    public void setInAppDismissTitle(String inAppDismissTitle) {
        this.inAppDismissTitle = inAppDismissTitle;
    }
}