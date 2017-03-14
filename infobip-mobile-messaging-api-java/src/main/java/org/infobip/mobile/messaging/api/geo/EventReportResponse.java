package org.infobip.mobile.messaging.api.geo;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Servers' response on reporting events. Contains statuses of registered campaigns.
 *
 * @see MobileApiGeo
 * @see MobileApiGeo#report(EventReportBody)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventReportResponse {
    Set<String> finishedCampaignIds;
    Set<String> suspendedCampaignIds;
    Map<String, String> messageIds;
}
