package org.infobip.mobile.messaging.geo.mapper;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.infobip.mobile.messaging.dal.bundle.BundleMapper;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoMessage;
import org.infobip.mobile.messaging.geo.report.GeoReport;

import java.util.List;

public class GeoBundleMapper extends BundleMapper {

    private static final String BUNDLED_GEO_TAG = GeoBundleMapper.class.getName() + ".geo";
    private static final String BUNDLED_GEO_MESSAGE_TAG = GeoBundleMapper.class.getName() + ".geo.message";
    private static final String BUNDLED_GEO_REPORTS_TAG = GeoBundleMapper.class.getName() + ".geo.report";

    /**
     * De-serializes Geo object from bundle
     *
     * @param bundle where to load data from
     * @return new geo object
     */
    @Nullable
    public static Geo geoFromBundle(@NonNull Bundle bundle) {
        return objectFromBundle(bundle, BUNDLED_GEO_TAG, Geo.class);
    }

    /**
     * Serializes geo message object into bundle
     *
     * @param geoMessage object to serialize
     * @return bundle with geo message contents
     */
    @NonNull
    public static Bundle geoMessageToBundle(@NonNull GeoMessage geoMessage) {
        return objectToBundle(geoMessage, BUNDLED_GEO_MESSAGE_TAG);
    }

    /**
     * De-serializes geo message object from bundle
     *
     * @param bundle where to load data from
     * @return new geo message object
     */
    @Nullable
    public static GeoMessage geoMessageFromBundle(@NonNull Bundle bundle) {
        return objectFromBundle(bundle, BUNDLED_GEO_MESSAGE_TAG, GeoMessage.class);
    }

    /**
     * Serializes geo object into bundle
     *
     * @param geo object to serialize
     * @return bundle with geo contents
     */
    @NonNull
    public static Bundle geoToBundle(@NonNull Geo geo) {
        return objectToBundle(geo, BUNDLED_GEO_TAG);
    }

    /**
     * De-serializes geo reports from bundle
     *
     * @param bundle where to load data from
     * @return new geo report object
     */
    @NonNull
    public static List<GeoReport> geoReportsFromBundle(@NonNull Bundle bundle) {
        return objectsFromBundle(bundle, BUNDLED_GEO_REPORTS_TAG, GeoReport.class);
    }

    /**
     * Serializes geo reports into bundle
     *
     * @param reports geo reports to serialize
     * @return bundle with geo reports' contents
     */
    @NonNull
    public static Bundle geoReportsToBundle(@NonNull List<GeoReport> reports) {
        return objectsToBundle(reports, BUNDLED_GEO_REPORTS_TAG);
    }
}
