package org.infobip.mobile.messaging.geo;

import android.annotation.SuppressLint;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.api.geo.EventReportBody;
import org.infobip.mobile.messaging.api.geo.MobileApiGeo;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.geo.report.GeoReporter;
import org.infobip.mobile.messaging.geo.tools.MobileMessagingTestCase;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendCommunicationException;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author pandric on 08/03/2017.
 */

public class RetryableSynchronizerTest extends MobileMessagingTestCase {

    private Executor executor;
    private MobileApiGeo mobileApiGeo;

    private GeoReporter geoReporter;

    @SuppressLint("CommitPrefEdits")
    @Override
    public void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.REPORT_SYSTEM_INFO, true);
        PreferenceHelper.saveInt(context, MobileMessagingProperty.DEFAULT_EXP_BACKOFF_MULTIPLIER, 0);
        PreferenceHelper.remove(context, MobileMessagingProperty.REPORTED_SYSTEM_DATA_HASH);

        executor = Executors.newSingleThreadExecutor();
        mobileApiGeo = mock(MobileApiGeo.class);
        geoReporter = new GeoReporter(context, mobileMessagingCore, geoBroadcaster, mobileMessagingCore.getStats(), mobileApiGeo);
    }

    @Test
    public void test_geo_report_retry() {

        // Given
        createReport(context, "signalingMessageId1", "campaignId1", "messageId1", true, createArea("areaId1"));
        createReport(context, "signalingMessageId2", "campaignId2", "messageId2", true, createArea("areaId2"));
        createReport(context, "signalingMessageId2", "campaignId2", "messageId3", true, createArea("areaId3"));
        given(mobileApiGeo.report(any(EventReportBody.class))).willThrow(new BackendCommunicationException("Backend error", new ApiIOException("0", "Backend error")));

        // When
        geoReporter.synchronize();

        // Then
        verify(geoBroadcaster, Mockito.after(8000).times(1)).error(any(MobileMessagingError.class));
        verify(mobileApiGeo, times(4)).report(any(EventReportBody.class));
    }
}
