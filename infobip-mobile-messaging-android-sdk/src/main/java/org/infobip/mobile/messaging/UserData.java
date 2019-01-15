package org.infobip.mobile.messaging;

import android.os.Bundle;

import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.util.DateTimeUtil;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.infobip.mobile.messaging.UserDataMapper.fromBundle;
import static org.infobip.mobile.messaging.UserDataMapper.mapEmailsToBackend;
import static org.infobip.mobile.messaging.UserDataMapper.mapGsmsToBackend;

public class UserData extends CustomAttributeHolder {

    private String externalUserId;
    private String firstName;
    private String lastName;
    private String middleName;
    private Gender gender;
    private String birthday;
    private Set<String> gsms;
    private Set<String> emails;
    private Set<String> tags;
    private List<Installation> installations;

    public enum Gender {
        Male, Female
    }

    public UserData() {

    }

    public UserData(String externalUserId,
                    String firstName,
                    String lastName,
                    String middleName,
                    Gender gender,
                    String birthday,
                    Set<String> gsms,
                    Set<String> emails,
                    Set<String> tags,
                    List<Installation> installations,
                    Map<String, CustomUserDataValue> customAttributes) {
        super(customAttributes);
        this.externalUserId = externalUserId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.gender = gender;
        this.birthday = birthday;
        this.gsms = gsms;
        this.emails = emails;
        this.tags = tags;
        this.installations = installations;
    }

    public static UserData createFrom(Bundle bundle) {
        return fromBundle(BroadcastParameter.EXTRA_USER_DATA, bundle);
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
     * @see MobileMessaging#saveUserData(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUserData(UserData)
     */
    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
        setField(UserAtts.externalUserId, externalUserId);
    }

    /**
     * Gets user's GSMs.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public Set<String> getGsms() {
        return this.gsms;
    }

    /**
     * Sets user's GSMs.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUserData(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUserData(UserData)
     */
    public void setGsms(Set<String> gsms) {
        this.gsms = gsms;
        setField(UserAtts.gsms, mapGsmsToBackend(gsms));
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
     * @see MobileMessaging#saveUserData(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUserData(UserData)
     */
    public void setEmails(Set<String> emails) {
        this.emails = emails;
        setField(UserAtts.emails, mapEmailsToBackend(emails));
    }

    /**
     * Gets user's tags.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public Set<String> getTags() {
        return tags;
    }

    /**
     * Sets user's tags.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUserData(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUserData(UserData)
     */
    public void setTags(Set<String> tags) {
        this.tags = tags;
        setField(UserAtts.tags, tags);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
        setField(UserAtts.firstName, firstName);
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
        setField(UserAtts.lastName, lastName);
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
        setField(UserAtts.middleName, middleName);
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
        setField(UserAtts.gender, gender != null ? gender.name() : gender);
    }

    public Date getBirthday() {
        try {
            return DateTimeUtil.DateFromYMDString(birthday);
        } catch (ParseException e) {
            return null;
        }
    }

    public void setBirthday(Date birthday) {
        this.birthday = DateTimeUtil.DateToYMDString(birthday);
        setField(UserAtts.birthday, this.birthday);
    }

    String getBirthdayString() {
        return birthday;
    }

    void setBirthdayString(String birthday) {
        this.birthday = birthday;
        setField(UserAtts.birthday, birthday);
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
