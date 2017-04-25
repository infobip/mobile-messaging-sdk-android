package org.infobip.mobile.messaging.api.messages;

import org.infobip.mobile.messaging.api.messages.reporting.MessageReport;
import org.infobip.mobile.messaging.api.messages.reporting.MessageStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Seen messages report body encapsulation.
 *
 * @author sslavin
 * @see MobileApiMessages
 * @see MobileApiMessages#reportSeen(SeenBody)
 * @since 25.04.2016.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeenBody {
    /**
     * Status being reported.
     */
    private final MessageStatus status = MessageStatus.SEEN;

    /**
     * Array of messages to report.
     */
    private MessageReport messages[];
}
