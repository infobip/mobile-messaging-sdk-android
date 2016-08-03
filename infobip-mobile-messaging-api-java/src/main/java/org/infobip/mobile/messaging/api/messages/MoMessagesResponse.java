package org.infobip.mobile.messaging.api.messages;

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
public class MoMessagesResponse {
    MoMessageDelivery messages[];
}
