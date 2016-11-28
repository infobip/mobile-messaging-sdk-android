package org.infobip.mobile.messaging.mobile.geo;


import org.infobip.mobile.messaging.api.geo.CampaignStatusEventResponse;
import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;

public class GeoReportingResult extends UnsuccessfulResult {

    private final String[] finishedCampaignIds;
    private final String[] suspendedCampaignIds;

    public GeoReportingResult(Throwable exception) {
        super(exception);
        finishedCampaignIds = null;
        suspendedCampaignIds = null;
    }

    public GeoReportingResult(CampaignStatusEventResponse campaignStatusEventResponse) {
        super(null);
        this.finishedCampaignIds = campaignStatusEventResponse.getFinishedCampaignIds();
        this.suspendedCampaignIds = campaignStatusEventResponse.getSuspendedCampaignIds();
    }

    public String[] getFinishedCampaignIds() {
        return finishedCampaignIds;
    }

    public String[] getSuspendedCampaignIds() {
        return suspendedCampaignIds;
    }
}
