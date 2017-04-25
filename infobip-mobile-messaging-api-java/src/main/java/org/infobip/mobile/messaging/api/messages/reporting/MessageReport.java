package org.infobip.mobile.messaging.api.messages.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

/**
 * Message status report encapsulation for delivery and seen reports.
 *
 * @author sslavin
 * @since 20/04/2017.
 */
@Data
@AllArgsConstructor
public class MessageReport {

    /**
     * Id of a message.
     */
    @NonNull
    private final String messageId;

    /**
     * Time delta in seconds.
     */
    private long timestampDelta;
}
