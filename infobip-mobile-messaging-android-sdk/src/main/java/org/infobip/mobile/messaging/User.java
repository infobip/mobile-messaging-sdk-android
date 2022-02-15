package org.infobip.mobile.messaging;

import android.os.Bundle;
import androidx.annotation.Nullable;

import org.infobip.mobile.messaging.api.appinstance.UserAtts;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.infobip.mobile.messaging.UserMapper.fromBundle;
import static org.infobip.mobile.messaging.UserMapper.mapEmailsToBackend;
import static org.infobip.mobile.messaging.UserMapper.mapPhonesToBackend;

public class User extends UserAttributes {

    private String externalUserId;
    private Set<String> phones;
    private Set<String> emails;
    private List<Installation> installations;

    public User() {

    }

    public User(@Nullable UserAttributes userAttributes) {
        setUserAtts(userAttributes);
    }

    public User(UserIdentity userIdentity) {
        setUserIdentity(userIdentity);
    }

    public User(UserIdentity userIdentity, UserAttributes userAttributes) {
        setUserIdentity(userIdentity);
        setUserAtts(userAttributes);
    }

    private void setUserIdentity(UserIdentity userIdentity) {
        if (userIdentity.containsField(UserAtts.externalUserId)) setExternalUserId(userIdentity.getExternalUserId());
        if (userIdentity.containsField(UserAtts.emails)) setEmails(userIdentity.getEmails());
        if (userIdentity.containsField(UserAtts.phones)) setPhones(userIdentity.getPhones());
    }

    private void setUserAtts(@Nullable UserAttributes userAttributes) {
        if (userAttributes == null) return;

        if (userAttributes.containsField(UserAtts.birthday)) setBirthdayString(userAttributes.getBirthdayString());
        if (userAttributes.containsField(UserAtts.firstName)) setFirstName(userAttributes.getFirstName());
        if (userAttributes.containsField(UserAtts.lastName)) setLastName(userAttributes.getLastName());
        if (userAttributes.containsField(UserAtts.middleName)) setMiddleName(userAttributes.getMiddleName());
        if (userAttributes.containsField(UserAtts.gender)) setGender(userAttributes.getGender());
        if (userAttributes.containsField(UserAtts.tags)) setTags(userAttributes.getTags());
    }

    public User(String externalUserId,
                String firstName,
                String lastName,
                String middleName,
                Gender gender,
                String birthday,
                Set<String> phones,
                Set<String> emails,
                Set<String> tags,
                List<Installation> installations,
                Map<String, CustomAttributeValue> customAttributes) {
        super(customAttributes);
        this.externalUserId = externalUserId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.gender = gender;
        this.birthday = birthday;
        this.phones = phones;
        this.emails = emails;
        this.tags = tags;
        this.installations = installations;
    }

    public static User createFrom(Bundle bundle) {
        return fromBundle(BroadcastParameter.EXTRA_USER, bundle);
    }

    /// region STANDARD ATTRIBUTES

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

    public List<Installation> getInstallations() {
        return installations;
    }

    void setInstallations(List<Installation> installations) {
        this.installations = installations;
        // no "setField()" because not syncing this to backend
    }

    /// endregion
}
