package org.infobip.mobile.messaging.mobile.geo;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.geo.CampaignStatusEventResponse;
import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventReports;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;

/**
 * @author sslavin
 * @since 20/10/2016.
 */
class GeoReportingTask extends AsyncTask<GeoReport, Void, GeoReportingResult> {

    private final Context context;

    GeoReportingTask(Context context) {
        this.context = context;
    }

    @Override
    protected GeoReportingResult doInBackground(GeoReport... geoReports) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);

        EventReport[] reports = GeoReporter.prepareEventReport(geoReports);

        try {
            CampaignStatusEventResponse eventResponse = MobileApiResourceProvider.INSTANCE.getMobileApiGeo(context).report(new EventReports(reports));
            return new GeoReportingResult(eventResponse);

        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            Log.e(MobileMessaging.TAG, "Error reporting geo areas!", e);
            cancel(true);

            return new GeoReportingResult(e);
        }
    }
}