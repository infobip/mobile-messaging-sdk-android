package org.infobip.mobile.messaging.api.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author sslavin
 * @since 21/07/16.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class MoMessageDelivery extends MoMessage {
    int statusCode;
    String status;
}
