package org.infobip.mobile.messaging.api.geo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Servers' response on reporting events. Contains statuses of registered campaigns.
 *
 * @see MobileApiGeo
 * @see MobileApiGeo#report(EventReports)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignStatusEventResponse {

    String[] finishedCampaignIds;
    String[] suspendedCampaignIds;
}
