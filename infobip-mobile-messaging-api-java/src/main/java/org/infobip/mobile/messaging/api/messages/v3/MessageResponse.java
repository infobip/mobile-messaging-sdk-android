package org.infobip.mobile.messaging.api.messages.v3;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author pandric on 09/09/16.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    @SerializedName("gcm.notification.messageId")
    String messageId;

    @SerializedName("gcm.notification.title")
    String title;

    @SerializedName("gcm.notification.body")
    String body;

    @SerializedName("gcm.notification.sound")
    String sound;

    @SerializedName("gcm.notification.vibrate")
    String vibrate;

    @SerializedName("gcm.notification.silent")
    String silent;

    @SerializedName("gcm.notification.category")
    String category;

    @Expose
    String customPayload;

    @Expose
    String internalData;
}
