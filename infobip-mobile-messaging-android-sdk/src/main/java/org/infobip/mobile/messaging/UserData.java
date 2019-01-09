package org.infobip.mobile.messaging;

import android.os.Bundle;

import org.infobip.mobile.messaging.api.appinstance.AppInstanceWithPushRegId;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.StringUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
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

    private static final JsonSerializer serializer = new JsonSerializer(true);

    private transient List<String> gsms;
    private transient List<String> emails;
    private transient Set<String> tags;
    private Map<String, Object> standardAttributes;
    private Map<String, CustomUserDataValue> customAttributes;
    private List<UserData.Installation> installations;

    public UserData() {
        this.standardAttributes = new HashMap<>();
        this.customAttributes = null;
        this.installations = new ArrayList<>();
    }

    public UserData(String userData) {
        UserData data = serializer.deserialize(userData, UserData.class);
        this.standardAttributes = data.standardAttributes;
        this.customAttributes = data.customAttributes;
        this.installations = data.installations;
    }

    protected UserData(Map<String, Object> standardAttributes,
                       Map<String, CustomUserDataValue> customAttributes,
                       List<UserData.Installation> installations) {
        this.standardAttributes = standardAttributes;
        this.customAttributes = customAttributes;
        this.installations = installations;
    }

    public static UserData createFrom(Bundle bundle) {
        return new UserData(bundle.getString(EXTRA_USER_DATA));
    }

    // CUSTOM ATTRIBUTES

    public void setCustomAttributes(Map<String, CustomUserDataValue> customAttributes) {
        if (customAttributes == null) {
            customAttributes = new HashMap<>();
        }
        this.customAttributes = customAttributes;
    }

    public Map<String, CustomUserDataValue> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomUserDataElement(String key, CustomUserDataValue customUserDataValue) {
        if (customAttributes == null) this.customAttributes = new HashMap<>();
        this.customAttributes.put(key, customUserDataValue);
    }

    public CustomUserDataValue getCustomUserDataValue(String key) {
        if (customAttributes == null) return null;
        return this.customAttributes.get(key);
    }

    public void removeCustomUserDataElement(String key) {
        if (this.customAttributes != null) {
            this.customAttributes.put(key, null);
        }
    }

    // CUSTOM ATTRIBUTES


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
        if (this.gsms != null && !this.gsms.isEmpty()) {
            return this.gsms;
        }

        Collection<String> gsms = getField(StandardAttribute.GSMS);
        if (gsms == null) {
            this.gsms = null;
            return Collections.emptyList();
        }

        this.gsms = new ArrayList<>(gsms);
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
        setField(StandardAttribute.GSMS, gsms);
        this.gsms = null;
        getGsms();
    }

    /**
     * Gets user's emails.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public List<String> getEmails() {
        if (this.emails != null && !this.emails.isEmpty()) {
            return this.emails;
        }

        Collection<String> emails = getField(StandardAttribute.EMAILS);
        if (emails == null) {
            this.emails = null;
            return Collections.emptyList();
        }

        this.emails = new ArrayList<>(emails);
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
        setField(StandardAttribute.EMAILS, emails);
        this.emails = null;
        getEmails();
    }

    /**
     * Gets user's tags.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public Set<String> getTags() {
        if (this.tags != null && !this.tags.isEmpty()) {
            return this.tags;
        }

        Collection<String> tags = getField(StandardAttribute.TAGS);
        if (tags == null) {
            this.tags = null;
            return Collections.emptySet();
        }
        this.tags = new HashSet<>(tags);
        return this.tags;
    }

    /**
     * Sets user's tags.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUserData(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUserData(UserData)
     */
    public void setTags(Set<String> tags) {
        setField(StandardAttribute.TAGS, tags != null ? new ArrayList<>(tags) : null);
        this.tags.clear();
        getTags();
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
        String gender = getField(StandardAttribute.GENDER);
        if (StringUtils.isNotBlank(gender)) {
            return Gender.valueOf(gender);
        }
        return null;
    }

    public void setGender(Gender gender) {
        if (gender != null) {
            setField(StandardAttribute.GENDER, gender.name());
        }
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
        return this.installations;
    }

    public void setInstallations(List<Installation> installations) {
        this.installations = installations;
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
        EXTERNAL_USER_ID("externalUserId"),
        TAGS("tags");

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

        protected String pushRegistrationId;
        protected Boolean isPrimaryDevice;
        protected Boolean isPushRegistrationEnabled;
        protected String deviceManufacturer;
        protected String deviceModel;
        protected String deviceName;
        protected String os;
        protected String osVersion;
        protected Boolean notificationsEnabled;

        public Installation() {
        }

        public static Installation createFrom(AppInstanceWithPushRegId instance) {
            return new Installation(
                    instance.getPushRegId(),
                    instance.getIsPrimary(),
                    instance.getRegEnabled(),
                    instance.getDeviceManufacturer(),
                    instance.getDeviceModel(),
                    instance.getDeviceName(),
                    instance.getOs(),
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
                            String os,
                            String osVersion,
                            Boolean notificationsEnabled) {
            this.pushRegistrationId = pushRegistrationId;
            this.isPrimaryDevice = isPrimaryDevice;
            this.isPushRegistrationEnabled = isPushRegistrationEnabled;
            this.deviceManufacturer = deviceManufacturer;
            this.deviceModel = deviceModel;
            this.deviceName = deviceName;
            this.os = os;
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

        public String getOs() {
            return os;
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
