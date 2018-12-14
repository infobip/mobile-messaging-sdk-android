package org.infobip.mobile.messaging;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.infobip.mobile.messaging.api.appinstance.AppInstanceWithPushRegId;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.DateTimeUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_USER_DATA;

/**
 * User data is used to provide user details to the server.
 * <br>
 * User data supports set of standard/predefined attributes ({@link StandardAttribute}) and custom attributes.
 * In order to delete any field, set it to null and report it to server.
 *
 * @author sslavin
 * @since 15/07/16.
 */
public class UserData {

    private static final JsonSerializer serializer = new JsonSerializer();

    private transient String externalUserId;
    private transient Set<String> tags;
    private transient List<String> gsms;
    private transient List<String> emails;
    private transient String preferredEmail;
    private transient String preferredGsm;
    private Map<String, Object> standardAttributes;
    private Map<String, CustomUserDataValue> customAttributes;
    private transient List<Installation> installations;

    public UserData() {
        this.externalUserId = null;
        this.tags = new HashSet<>();
        this.gsms = new ArrayList<>();
        this.emails = new ArrayList<>();
        this.standardAttributes = new HashMap<>();
        this.customAttributes = new HashMap<>();
        this.installations = new ArrayList<>();
    }

    public UserData(String userData) {
        UserData data = serializer.deserialize(userData, UserData.class);
        this.externalUserId = data.externalUserId;
        this.tags = data.tags;
        this.gsms = data.gsms;
        this.emails = data.emails;
        this.standardAttributes = data.standardAttributes;
        this.customAttributes = data.customAttributes;
    }

    protected UserData(String externalUserId,
                       Set<String> tags,
                       List<String> gsms,
                       List<String> emails,
                       Map<String, Object> standardAttributes,
                       Map<String, CustomUserDataValue> customAttributes) {
        this.externalUserId = externalUserId;
        this.tags = tags;
        this.gsms = gsms;
        this.emails = emails;
        this.standardAttributes = standardAttributes;
        this.customAttributes = customAttributes;
    }

    @Nullable
    public static UserData merge(UserData old, UserData latest) {
        if (old == null && latest == null) {
            return null;
        }

        UserData merged = new UserData();
        merged.add(old);
        merged.add(latest);
        return merged;
    }

    public static UserData createFrom(Bundle bundle) {
        return new UserData(bundle.getString(EXTRA_USER_DATA));
    }

    private void add(UserData data) {
        if (data == null) {
            return;
        }

        if (data.standardAttributes != null) {
            this.standardAttributes.putAll(data.standardAttributes);
        }

        if (data.customAttributes != null) {
            this.customAttributes.putAll(data.customAttributes);
        }

        if (data.emails != null) {
            this.emails.addAll(data.emails);
        }

        if (data.gsms != null) {
            this.gsms.addAll(data.gsms);
        }

        if (data.tags != null) {
            this.tags.addAll(data.tags);
        }

        this.externalUserId = data.externalUserId;
    }

    public void setCustomAttributes(Map<String, CustomUserDataValue> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public Map<String, CustomUserDataValue> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomUserDataElement(String key, CustomUserDataValue customUserDataValue) {
        this.customAttributes.put(key, customUserDataValue);
    }

    public CustomUserDataValue getCustomUserDataValue(String key) {
        return this.customAttributes.get(key);
    }

    public void removeCustomUserDataElement(String key) {
        this.customAttributes.put(key, null);
    }


    // STANDARD ATTRIBUTES

    /**
     * Returns user's standard (predefined) attributes (all possible attributes are described in the {@link StandardAttribute} enum). Predefined attributes that are related to a particular user. You can provide additional users information to the server, so that you will be able to send personalised targeted messages to exact user.
     */
    public Map<String, Object> getStandardAttributes() {
        return standardAttributes;
    }

    /**
     * Gets external user ID - the user's ID you can provide in order to <b>link your own unique user identifier</b> with Mobile Messaging user id,
     * so that you will be able to send personalised targeted messages to the exact user.
     */
    public String getExternalUserId() {
        return getField(StandardAttribute.EXTERNAL_USER_ID);
    }

    /**
     * Sets external user ID - the user's ID you can provide in order to <b>link your own unique user identifier</b> with Mobile Messaging user id,
     * so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUserData(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUserData(UserData)
     */
    public void setExternalUserId(String externalUserId) {
        setField(StandardAttribute.EXTERNAL_USER_ID, externalUserId);
    }

    /**
     * Gets user's GSMs.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public List<String> getGsms() {
        if (this.gsms == null) {
            List<UserBody.Gsm> gsmsWithPreferred = getGsmsWithPreferred();
            if (gsmsWithPreferred != null) {
                for (UserBody.Gsm gsm : gsmsWithPreferred) {
                    this.gsms.add(gsm.getNumber());
                    if (gsm.getPreferred()) this.preferredGsm = gsm.getNumber();
                }
            }
        }
        return this.gsms;
    }

    /**
     * Sets user's GSMs.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUserData(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUserData(UserData)
     */
    public void setGsms(List<String> gsms) {
        if (gsms == null) gsms = Collections.emptyList();
        this.gsms = gsms;

        List<UserBody.Gsm> gsmsWithPreferred = getGsmsWithPreferred();
        if (gsmsWithPreferred == null) gsmsWithPreferred = new ArrayList<>(gsms.size());
        for (String gsm : gsms) {
            if (TextUtils.isEmpty(gsm)) continue;
            gsmsWithPreferred.add(new UserBody.Gsm(gsm, gsm.equals(preferredGsm)));
        }
        setField(StandardAttribute.GSMS, gsmsWithPreferred);
    }

    /**
     * Gets user's preferred GSM.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public String getPreferredGsm() {
        if (this.preferredGsm == null) {
            List<UserBody.Gsm> gsmsWithPreferred = getGsmsWithPreferred();
            if (gsmsWithPreferred != null) {
                for (UserBody.Gsm gsm : gsmsWithPreferred) {
                    if (gsm.getPreferred()) this.preferredGsm = gsm.getNumber();
                }
            }
        }
        return this.preferredGsm;
    }

    protected List<UserBody.Gsm> getGsmsWithPreferred() {
        return getField(StandardAttribute.GSMS);
    }

    protected void setGsmsWithPreferred(List<UserBody.Gsm> gsms) {
        if (gsms == null) gsms = Collections.emptyList();
        if (this.gsms == null) this.gsms = new ArrayList<>(gsms.size());
        setField(StandardAttribute.GSMS, gsms);
    }

    /**
     * Gets user's emails.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public List<String> getEmails() {
        if (this.emails == null) {
            List<UserBody.Email> emailsWithPreferred = getEmailsWithPreferred();
            if (emailsWithPreferred != null) {
                for (UserBody.Email email : emailsWithPreferred) {
                    this.emails.add(email.getAddress());
                    if (email.getPreferred()) this.preferredEmail = email.getAddress();
                }
            }
        }
        return this.emails;
    }

    /**
     * Sets user's emails.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUserData(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUserData(UserData)
     */
    public void setEmails(List<String> emails) {
        if (emails == null) emails = Collections.emptyList();
        this.emails = emails;

        List<UserBody.Email> emailsWithPreferred = getEmailsWithPreferred();
        for (String email : emails) {
            if (TextUtils.isEmpty(email)) continue;
            emailsWithPreferred.add(new UserBody.Email(email, email.equals(preferredEmail)));
        }
        setField(StandardAttribute.EMAILS, emailsWithPreferred);
    }

    /**
     * Gets user's preferred email.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public String getPreferredEmail() {
        if (this.preferredEmail == null) {
            List<UserBody.Email> emailsWithPreferred = getEmailsWithPreferred();
            if (emailsWithPreferred != null) {
                for (UserBody.Email email : emailsWithPreferred) {
                    if (email.getPreferred()) this.preferredEmail = email.getAddress();
                }
            }
        }
        return this.preferredEmail;
    }

    protected List<UserBody.Email> getEmailsWithPreferred() {
        return getField(StandardAttribute.EMAILS);
    }

    protected void setEmailsWithPreferred(List<UserBody.Email> emails) {
        if (emails == null) emails = Collections.emptyList();
        setField(StandardAttribute.EMAILS, emails);
    }

    /**
     * Gets user's tags.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public Set<String> getTags() {
        Object tagsField = getField(StandardAttribute.TAGS);
        if (tagsField instanceof List) {
            return new HashSet<>((List<String>) tagsField);
        } else {
            return (HashSet<String>) tagsField;
        }
    }

    /**
     * Sets user's tags.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUserData(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUserData(UserData)
     */
    public void setTags(Set<String> tags) {
        if (tags == null) tags = new HashSet<>(0);
        setField(StandardAttribute.TAGS, tags);
    }

    public String getFirstName() {
        return getField(StandardAttribute.FIRST_NAME);
    }

    public void setFirstName(String firstName) {
        setField(StandardAttribute.FIRST_NAME, firstName);
    }

    public String getLastName() {
        return getField(StandardAttribute.LAST_NAME);
    }

    public void setLastName(String lastName) {
        setField(StandardAttribute.LAST_NAME, lastName);
    }

    public String getMiddleName() {
        return getField(StandardAttribute.MIDDLE_NAME);
    }

    public void setMiddleName(String middleName) {
        setField(StandardAttribute.MIDDLE_NAME, middleName);
    }

    public Gender getGender() {
        return getField(StandardAttribute.GENDER);
    }

    public void setGender(Gender gender) {
        setField(StandardAttribute.GENDER, gender);
    }

    public Date getBirthday() {
        try {
            return DateTimeUtil.DateFromYMDString((String) getField(StandardAttribute.BIRTHDAY));
        } catch (ParseException e) {
            return null;
        }
    }

    public void setBirthday(Date birthday) {
        String ymdString = DateTimeUtil.DateToYMDString(birthday);
        setField(StandardAttribute.BIRTHDAY, ymdString);
    }

    public List<Installation> getInstallations() {
        return getField(StandardAttribute.INSTALLATIONS);
    }

    //TODO
    public void setInstallations(List<Installation> installations) {
        setField(StandardAttribute.INSTALLATIONS, installations);
    }

    // STANDARD ATTRIBUTES

    private <T> T getField(StandardAttribute field) {
        if (standardAttributes == null) {
            return null;
        }

        Object object = standardAttributes.get(field.getKey());
        try {
            return (T) object;
        } catch (ClassCastException e) {
            return null;
        }
    }

    private void setField(StandardAttribute field, Object value) {
        standardAttributes.put(field.getKey(), value);
    }

    public enum StandardAttribute {
        FIRST_NAME("firstName"),
        LAST_NAME("lastName"),
        MIDDLE_NAME("middleName"),
        GENDER("gender"),
        BIRTHDAY("birthday"),
        GSMS("gsms"),
        EMAILS("emails"),
        TAGS("tags"),
        EXTERNAL_USER_ID("externalUserId"),
        INSTALLATIONS("installations");

        private final String key;

        StandardAttribute(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public enum Gender {
        Male, Female
    }

    public static class Installation {

        private String pushRegistrationId;
        private Boolean isPrimaryDevice;
        private Boolean isPushRegistrationEnabled;
        private String deviceManufacturer;
        private String deviceModel;
        private String deviceName;
        private String osVersion;
        private Boolean notificationsEnabled;

        public static Installation createFrom(AppInstanceWithPushRegId instance) {
            return new Installation(
                    instance.getPushRegId(),
                    instance.getIsPrimary(),
                    instance.getRegEnabled(),
                    instance.getDeviceManufacturer(),
                    instance.getDeviceModel(),
                    instance.getDeviceName(),
                    instance.getOsVersion(),
                    instance.getNotificationsEnabled()
            );
        }

        public Installation(String pushRegistrationId,
                            Boolean isPrimaryDevice,
                            Boolean isPushRegistrationEnabled,
                            String deviceManufacturer,
                            String deviceModel,
                            String deviceName,
                            String osVersion,
                            Boolean notificationsEnabled) {
            this.pushRegistrationId = pushRegistrationId;
            this.isPrimaryDevice = isPrimaryDevice;
            this.isPushRegistrationEnabled = isPushRegistrationEnabled;
            this.deviceManufacturer = deviceManufacturer;
            this.deviceModel = deviceModel;
            this.deviceName = deviceName;
            this.osVersion = osVersion;
            this.notificationsEnabled = notificationsEnabled;
        }

        public String getPushRegistrationId() {
            return pushRegistrationId;
        }

        public Boolean getPrimaryDevice() {
            return isPrimaryDevice;
        }

        public void setPrimaryDevice(Boolean isPrimaryDevice) {
            this.isPrimaryDevice = isPrimaryDevice;
        }

        public Boolean isPushRegistrationEnabled() {
            return isPushRegistrationEnabled;
        }

        public String getDeviceManufacturer() {
            return deviceManufacturer;
        }

        public String getDeviceModel() {
            return deviceModel;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public Boolean getNotificationsEnabled() {
            return notificationsEnabled;
        }

        @Override
        public String toString() {
            return serializer.serialize(this);
        }
    }

    @Override
    public String toString() {
        return serializer.serialize(this);
    }
}
