/*
 * UserIdentity.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.api.support.MapModel;

import java.util.Set;

import static org.infobip.mobile.messaging.UserMapper.mapEmailsToBackend;
import static org.infobip.mobile.messaging.UserMapper.mapPhonesToBackend;

public class UserIdentity extends MapModel {

    private String externalUserId;
    private Set<String> phones;
    private Set<String> emails;

    public UserIdentity() {

    }

    /**
     * Gets external user ID - the user's ID you can provide in order to <b>link your own unique user identifier</b> with Mobile Messaging user id,
     * so that you will be able to send personalised targeted messages to the exact user.
     */
    public String getExternalUserId() {
        return externalUserId;
    }

    /**
     * Sets external user ID - the user's ID you can provide in order to <b>link your own unique user identifier</b> with Mobile Messaging user id,
     * so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUser(User, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUser(User)
     */
    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
        setField(UserAtts.externalUserId, externalUserId);
    }

    /**
     * Gets user's phone numbers.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public Set<String> getPhones() {
        return this.phones;
    }

    /**
     * Sets user's phone numbers.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUser(User, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUser(User)
     */
    public void setPhones(Set<String> phones) {
        this.phones = phones;
        setField(UserAtts.phones, mapPhonesToBackend(phones));
    }

    /**
     * Gets user's emails.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public Set<String> getEmails() {
        return emails;
    }

    /**
     * Sets user's emails.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUser(User, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUser(User)
     */
    public void setEmails(Set<String> emails) {
        this.emails = emails;
        setField(UserAtts.emails, mapEmailsToBackend(emails));
    }
}
