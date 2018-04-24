package org.infobip.mobile.messaging.chat;

import android.content.Intent;
import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.chat.broadcast.ChatBundleMapper;
import org.infobip.mobile.messaging.platform.Time;
import org.json.JSONObject;

import java.util.UUID;

/**
 * One simple message from chat
 *
 * @author sslavin
 * @since 05/10/2017.
 */

@SuppressWarnings({"unused", "WeakerAccess"})
public class ChatMessage implements Comparable<ChatMessage> {
    /**
     * Unique message id
     */
    private String id;

    /**
     * Message text
     */
    private String body;

    /**
     * ID of a chat which this message belongs to
     */
    private String chatId;

    /**
     * Date when this message was created
     */
    private long createdAt;

    /**
     * Date when this message was received
     */
    private long receivedAt;

    /**
     * Date when this message was seen
     */
    private long readAt;

    /**
     * Message category for actionable notifications
     */
    private String category;

    /**
     * Url for media content provided with message
     */
    private String contentUrl;

    /**
     * Author of this message
     */
    private ChatParticipant author;

    /**
     * Current status of message
     */
    private Message.Status status;

    /**
     * Any custom data attached to message
     */
    private JSONObject customData;

    /**
     * Flag that indicates that message belongs to current user
     */
    private boolean isYours;

    /**
     * Creates message based on provided intent
     *
     * @param intent intent with data
     * @return message object
     */
    public static ChatMessage createFrom(@NonNull Intent intent) {
        return mapper.chatMessageFromBundle(intent.getExtras());
    }

    @Override
    public int compareTo(@NonNull ChatMessage o) {
        if (createdAt != 0 && o.createdAt != 0) {
            return (int) Math.signum(createdAt - o.createdAt);
        }
        return (int) Math.signum(receivedAt - o.receivedAt);
    }

    // region Boilerplate code

    private final static ChatBundleMapper mapper = new ChatBundleMapper();

    public ChatMessage(String id, String body, String chatId, long createdAt, long receivedAt, long readAt, String category, String contentUrl, ChatParticipant author, Message.Status status, JSONObject customData, boolean isYours) {
        this.id = id;
        this.body = body;
        this.chatId = chatId;
        this.createdAt = createdAt;
        this.receivedAt = receivedAt;
        this.readAt = readAt;
        this.category = category;
        this.contentUrl = contentUrl;
        this.author = author;
        this.status = status;
        this.customData = customData;
        this.isYours = isYours;
    }

    public ChatMessage() {
        this(UUID.randomUUID().toString(),
                null,
                null,
                Time.now(),
                0,
                0,
                null,
                null,
                null,
                Message.Status.UNKNOWN,
                null,
                false);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(long receivedAt) {
        this.receivedAt = receivedAt;
    }

    public long getReadAt() {
        return readAt;
    }

    public void setReadAt(long readAt) {
        this.readAt = readAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public ChatParticipant getAuthor() {
        return author;
    }

    public void setAuthor(ChatParticipant author) {
        this.author = author;
    }

    public Message.Status getStatus() {
        return status;
    }

    public void setStatus(Message.Status status) {
        this.status = status;
    }

    public JSONObject getCustomData() {
        return customData;
    }

    public void setCustomData(JSONObject customData) {
        this.customData = customData;
    }

    public boolean isYours() {
        return isYours;
    }

    public void setYours(boolean isYours) {
        this.isYours = isYours;
    }

    // endregion
}
