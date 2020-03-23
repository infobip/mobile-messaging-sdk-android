package org.infobip.mobile.messaging.platform;

import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.User;
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
     * Sends broadcast that error occurred.
     *
     * @param error error to provide.
     */
    void error(MobileMessagingError error);

    /**
     * Sends broadcast with message ids which were reported as delivered by the library
     *
     * @param messageIds ids of messages marked as delivered
     */
    void deliveryReported(String... messageIds);

    /**
     * Sends broadcast with message ids which were reported as seen by the library
     *
     * @param messageIds ids of messages marked as seen
     */
    void seenStatusReported(String... messageIds);

    /**
     * Sends broadcast indicating that MO messages were sent to the server
     *
     * @param messages list of sent messages
     */
    void messagesSent(List<Message> messages);

    /**
     * Sends broadcast when new cloud token is received
     *
     * @param cloudToken FCM cloud token
     */
    void tokenReceived(String cloudToken);

    /**
     * Sends broadcast about new registration data
     *
     * @param cloudToken         FCM cloud token
     * @param pushRegistrationId Infobip registration id
     */
    void registrationCreated(String cloudToken, String pushRegistrationId);

    /**
     * Sends broadcast with installation data on installation update
     *
     * @param installation device instance
     */
    void installationUpdated(Installation installation);

    /**
     * Sends broadcast with user data
     *
     * @param user user data
     */
    void userUpdated(User user);

    /**
     * Sends broadcast that current installation is depersonalized - user's specific data is detached the installation
     */
    void depersonalized();

    /**
     * Sends broadcast that current installation is personalized - user's specific data is added
     */
    void personalized(User user);

    /**
     * Sends broadcast that user's sessions are reported
     */
    void userSessionsReported();

    /**
     * Sends broadcast that user's custom events are reported
     */
    void customEventsReported();
}
