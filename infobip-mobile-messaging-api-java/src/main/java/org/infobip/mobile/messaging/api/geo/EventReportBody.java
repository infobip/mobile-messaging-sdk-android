package org.infobip.mobile.messaging.api.geo;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author sslavin
 * @since 24/10/2016.
 */

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class EventReportBody {
    final String platformType = "GCM";

    Set<MessagePayload> messages;
    Set<EventReport> reports;
    String deviceApplicationInstanceId;
}
