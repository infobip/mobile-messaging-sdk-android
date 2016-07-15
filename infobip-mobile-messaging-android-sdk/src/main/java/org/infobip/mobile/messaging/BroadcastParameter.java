package org.infobip.mobile.messaging;

/**
 * @author pandric
 * @since 19.05.16.
 */
public final class BroadcastParameter {

    public static final String EXTRA_PARAMETER_NAME = "org.infobip.mobile.messaging.parameter.name";
    public static final String EXTRA_PARAMETER_VALUE = "org.infobip.mobile.messaging.parameter.value";
    public static final String EXTRA_EXCEPTION = "org.infobip.mobile.messaging.exception";
    public static final String EXTRA_MSISDN = "org.infobip.mobile.messaging.msisdn";
    public static final String EXTRA_GCM_TOKEN = "org.infobip.mobile.messaging.gcm.token";
    public static final String EXTRA_INFOBIP_ID = "org.infobip.mobile.messaging.infobip.token";
    public static final String EXTRA_MESSAGE_IDS = "org.infobip.mobile.messaging.message.ids";
    public static final String EXTRA_GEOFENCE_AREAS = "org.infobip.mobile.messaging.message.geofenceAreas";

    private BroadcastParameter() {
    }
}
