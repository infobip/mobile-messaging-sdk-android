package org.infobip.mobile.messaging.mobile.registration;

/**
 * @author sslavin
 * @since 21/08/2017.
 */

public class Registration {
    final String cloudToken;
    final String registrationId;
    final Boolean enabled;

    public Registration(String cloudToken, String registrationId, Boolean enabled) {
        this.cloudToken = cloudToken;
        this.registrationId = registrationId;
        this.enabled = enabled;
    }
}
