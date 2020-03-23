package org.infobip.mobile.messaging.geo;

import android.os.Bundle;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.geo.mapper.GeoBundleMapper;
import org.json.JSONObject;

import java.util.List;


public class GeoMessage extends Message {

    private Geo geo;

    public static GeoMessage createFrom(Message message, Geo geo) {
        return new GeoMessage(message.getMessageId(), message.getTitle(), message.getBody(), message.getSound(), message.isVibrate(), message.getIcon(),
                message.isSilent(), message.getCategory(), message.getFrom(), message.getReceivedTimestamp(), message.getSeenTimestamp(), message.getSentTimestamp(),
                message.getCustomPayload(), message.getInternalData(), message.getDestination(), message.getStatus(), message.getStatusMessage(), message.getContentUrl(),
                geo, message.getInAppStyle(), message.getInAppExpiryTimestamp());
    }

    public static GeoMessage createFrom(Bundle bundle) {
        return GeoBundleMapper.geoMessageFromBundle(bundle);
    }

    static Message toMessage(GeoMessage geoMessage) {
        return new Message(geoMessage.getMessageId(), geoMessage.getTitle(), geoMessage.getBody(), geoMessage.getSound(), geoMessage.isVibrate(), geoMessage.getIcon(),
                geoMessage.isSilent(), geoMessage.getCategory(), geoMessage.getFrom(), geoMessage.getReceivedTimestamp(), geoMessage.getSeenTimestamp(), geoMessage.getSentTimestamp(),
                geoMessage.getCustomPayload(), geoMessage.getInternalData(), geoMessage.getDestination(), geoMessage.getStatus(), geoMessage.getStatusMessage(), geoMessage.getContentUrl(),
                geoMessage.getInAppStyle(), geoMessage.getInAppExpiryTimestamp());
    }

    private GeoMessage(String messageId, String title, String body, String sound, boolean vibrate, String icon, boolean silent, String category,
                       String from, long receivedTimestamp, long seenTimestamp, long sentTimestamp, JSONObject customPayload, String internalData,
                       String destination, Status status, String statusMessage, String contentUrl, Geo geo, InAppStyle inAppStyle, long expiryTime) {
        super(messageId, title, body, sound, vibrate, icon, silent, category, from, receivedTimestamp, seenTimestamp, seenTimestamp, customPayload,
                internalData, destination, status, statusMessage, contentUrl, inAppStyle, expiryTime);
        this.geo = geo;
    }

    public Geo getGeo() {
        return geo;
    }

    public List<Area> getAllMonitoredAreas() {
        if (geo == null) {
            return null;
        }

        return geo.getAreasList();
    }

    public void setGeo(Geo geo) {
        this.geo = geo;
    }
}
