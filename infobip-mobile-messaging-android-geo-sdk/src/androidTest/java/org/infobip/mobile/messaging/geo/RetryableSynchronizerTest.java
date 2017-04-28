package org.infobip.mobile.messaging.geo;

import android.annotation.SuppressLint;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.geo.report.GeoReporter;
import org.infobip.mobile.messaging.geo.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author pandric on 08/03/2017.
 */

public class RetryableSynchronizerTest extends MobileMessagingTestCase {

    private Executor executor;

    private GeoReporter geoReporter;

    @SuppressLint("CommitPrefEdits")
    @Override
    public void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, true);
        PreferenceHelper.saveInt(context, MobileMessagingProperty.DEFAULT_EXP_BACKOFF_MULTIPLIER, 0);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);

        executor = Executors.newSingleThreadExecutor();
        geoReporter = new GeoReporter(context, geoBroadcaster, mobileMessagingCore.getStats());

        debugServer.respondWith(NanoHTTPD.Response.Status.INTERNAL_ERROR, "{\n" +
                "  \"code\": \"500\",\n" +
                "  \"message\": \"Internal server error\"\n" +
                "}");
    }

    @Test
    public void test_geo_report_retry() {

        // Given
        createReport(context, "signalingMessageId1", "campaignId1", "messageId1", true, createArea("areaId1"));
        createReport(context, "signalingMessageId2", "campaignId2", "messageId2", true, createArea("areaId2"));
        createReport(context, "signalingMessageId2", "campaignId2", "messageId3", true, createArea("areaId3"));

        // When
        geoReporter.synchronize();

        // Then
        Mockito.verify(geoBroadcaster, Mockito.after(8000).atLeast(4)).error(Mockito.any(MobileMessagingError.class));
    }
}
