package org.infobip.mobile.messaging.api.messages;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sslavin
 * @since 21/07/16.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoOutgoingMessage {
    String destination;
    String text;
    Map<String, Object> customPayload;
}
