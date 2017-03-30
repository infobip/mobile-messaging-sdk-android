package org.infobip.mobile.messaging.platform;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.SystemData;
import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.geo.Geo;
import org.infobip.mobile.messaging.geo.GeoEventType;
import org.infobip.mobile.messaging.geo.GeoReport;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;

import java.util.List;

/**
 * Sends broadcasts via context (globally) and LocalBroadcastManager (locally).
 *
 * @author sslavin
 * @since 13/03/2017.
 */

public interface Broadcaster {
    /**
     * Sends broadcast that message is received
     *
     * @param message received message
     */
    void messageReceived(Message message);

    /**
     * Sends broadcast that notification is tapped
     *
     * @param message received message
     */
    void notificationTapped(Message message);

    /**
     * Sends geo occured broadcast
     *
     * @param event event type
     * @param message generated message
     * @param geo geo information
     */
    void geoEvent(GeoEventType event, Message message, Geo geo);

    /**
     * Sends broadcast that geo events were reported to the server
     *
     * @param reports geo reports.
     */
    void geoReported(List<GeoReport> reports);

    /**
     * Sends broadcast that error occured.
     *
     * @param error error to provide.
     */
    void error(MobileMessagingError error);

    /**
     * Sends broadcast when new cloud token acquired
     * @param cloudToken GCM/FCM cloud token
     */
    void registrationAcquired(String cloudToken);

    /**
     * Sends broadcast about new registration data
     * @param cloudToken GCM/FCM cloud token
     * @param deviceApplicationInstanceId Infobip registraiton id
     */
    void registrationCreated(String cloudToken, String deviceApplicationInstanceId);

    /**
     * Sends broadcast when push registration enabled is available
     * @param cloudToken GCM/FCM cloud token
     * @param deviceInstanceId Infobip registration ID
     * @param registrationEnabled true if registration enabled
     */
    void registrationEnabled(String cloudToken, String deviceInstanceId, Boolean registrationEnabled);

    /**
     * Sends broadcast with message ids which were reported as delivered by the library
     * @param messageIds ids of messages marked as delivered
     */
    void deliveryReported(String...messageIds);

    /**
     * Sends broadcast with message ids which were reported as seen by the library
     * @param messageIds ids of messages marked as seen
     */
    void seenStatusReported(String...messageIds);

    /**
     * Sends broadacast indicating that MO messages were sent to the server
     * @param messages list of sent messages
     */
    void messagesSent(List<Message> messages);

    /**
     * Sends broadcast with user data
     * @param userData user data
     */
    void userDataReported(UserData userData);

    /**
     * Sends broadcast with system data
     * @param systemData system data
     */
    void systemDataReported(SystemData systemData);
}
