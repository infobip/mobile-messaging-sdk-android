package org.infobip.mobile.messaging.api.geo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sslavin
 * @since 19/10/2016.
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventReport {
    EventType event;
    String geoAreaId;
    String campaignId;
    String messageId;
    String sdkMessageId;
    Long timestampDelta;
}
