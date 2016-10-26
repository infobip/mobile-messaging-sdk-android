package org.infobip.mobile.messaging.mobile.geo;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.geo.EventReport;
import org.infobip.mobile.messaging.api.geo.EventReports;
import org.infobip.mobile.messaging.api.geo.EventType;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.UnsuccessfulResult;

import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 20/10/2016.
 */

class GeoReportingTask extends AsyncTask<GeoReport, Void, UnsuccessfulResult> {

    private final Context context;

    GeoReportingTask(Context context) {
        this.context = context;
    }

    @Override
    protected UnsuccessfulResult doInBackground(GeoReport... geoReports) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);

        EventReport reports[] = new EventReport[geoReports.length];
        for (int i = 0; i < reports.length; i++) {

            Long timestampDelta = System.currentTimeMillis() - geoReports[i].getTimestampOccured();
            Long timestampDeltaSeconds = TimeUnit.MILLISECONDS.toSeconds(timestampDelta);

            reports[i] = new EventReport(EventType.valueOf(geoReports[i].getEvent().name()), geoReports[i].getArea().getId(),
                    geoReports[i].getCampaignId(), geoReports[i].getMessageId(), timestampDeltaSeconds);
        }

        try {
            MobileApiResourceProvider.INSTANCE.getMobileApiGeo(context).report(new EventReports(reports));
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            Log.e(MobileMessaging.TAG, "Error reporting geo areas!", e);
            cancel(true);
            return new UnsuccessfulResult(e);
        }
        return null;
    }
}