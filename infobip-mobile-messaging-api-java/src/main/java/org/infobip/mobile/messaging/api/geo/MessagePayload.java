package org.infobip.mobile.messaging.api.geo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sslavin
 * @since 06/02/2017.
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessagePayload {
    String messageId;
    String title;
    String body;
    String sound;
    Boolean vibrate;
    String category;
    Boolean silent;

    String customPayload;
    String internalData;
}
