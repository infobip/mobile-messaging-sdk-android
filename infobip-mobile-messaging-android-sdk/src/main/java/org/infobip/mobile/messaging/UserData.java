package org.infobip.mobile.messaging;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import org.infobip.mobile.messaging.api.appinstance.AppInstanceWithPushRegId;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.StringUtils;

import java.lang.reflect.Type;
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

    private static final JsonSerializer serializer = new JsonSerializer(false);

    private transient String externalUserId;
    private transient List<String> gsms;
    private transient List<String> emails;
    private transient String preferredEmail;
    private transient String preferredGsm;
    private Set<String> tags;
    private Map<String, Object> standardAttributes;
    private Map<String, CustomUserDataValue> customAttributes;
    private List<UserData.Installation> installations;

    public UserData() {
        this.externalUserId = null;
        this.gsms = new ArrayList<>();
        this.emails = new ArrayList<>();
        this.tags = null;
        this.standardAttributes = new HashMap<>();
        this.customAttributes = null;
        this.installations = new ArrayList<>();
    }

    public UserData(String userData) {
        UserData data = serializer.deserialize(userData, UserData.class);
        this.externalUserId = data.externalUserId;
        this.gsms = data.gsms;
        this.emails = data.emails;
        this.tags = data.tags;
        this.standardAttributes = data.standardAttributes;
        this.customAttributes = data.customAttributes;
        this.installations = data.installations;
    }

    protected UserData(String externalUserId,
                       List<String> gsms,
                       List<String> emails,
                       Set<String> tags,
                       Map<String, Object> standardAttributes,
                       Map<String, CustomUserDataValue> customAttributes,
                       List<UserData.Installation> installations) {
        this.externalUserId = externalUserId;
        this.gsms = gsms;
        this.emails = emails;
        this.tags = tags;
        this.standardAttributes = standardAttributes;
        this.customAttributes = customAttributes;
        this.installations = installations;
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
            if (this.customAttributes == null) this.customAttributes = new HashMap<>();
            this.customAttributes.putAll(data.customAttributes);
        }

        if (data.emails != null) {
            this.emails.addAll(data.emails);
        }

        if (data.gsms != null) {
            this.gsms.addAll(data.gsms);
        }

        if (data.tags != null) {
            if (this.tags == null) this.tags = new HashSet<>();
            this.tags.addAll(data.tags);
        }

        if (data.installations != null) {
            this.installations.addAll(data.installations);
        }

        this.externalUserId = data.externalUserId;
    }

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
     * @see MobileMessaging#saveUser(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUser(UserData)
     */
    public void setExternalUserId(String externalUserId) {
        setField(StandardAttribute.EXTERNAL_USER_ID, externalUserId);
    }

    /**
     * Gets user's GSMs.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public List<String> getGsms() {
        if (this.gsms == null || this.gsms.isEmpty()) {
            HashSet<UserBody.Gsm> gsmsWithPreferred = getGsmsWithPreferred();
            if (gsmsWithPreferred != null) {
                for (UserBody.Gsm gsm : gsmsWithPreferred) {
                    this.gsms.add(gsm.getNumber());
                    Boolean preferredGsm = gsm.getPreferred();
                    if (preferredGsm != null && preferredGsm) this.preferredGsm = gsm.getNumber();
                }
            }
        }
        return this.gsms;
    }

    /**
     * Sets user's GSMs.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUser(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUser(UserData)
     */
    public void setGsms(List<String> gsms) {
        if (gsms == null) gsms = Collections.emptyList();
        this.gsms = gsms;

        HashSet<UserBody.Gsm> gsmsWithPreferred = getGsmsWithPreferred();
        if (gsmsWithPreferred == null) gsmsWithPreferred = new HashSet<>(gsms.size());
        for (String gsm : gsms) {
            if (TextUtils.isEmpty(gsm)) continue;
            gsmsWithPreferred.add(new UserBody.Gsm(gsm, gsm.equals(preferredGsm)));
        }
        setGsmsWithPreferred(gsmsWithPreferred);
    }

    protected HashSet<UserBody.Gsm> getGsmsWithPreferred() {
        Type type = new TypeToken<Set<UserBody.Gsm>>() {}.getType();
        return serializer.deserialize((String) getField(StandardAttribute.GSMS), type);
    }

    protected void setGsmsWithPreferred(HashSet<UserBody.Gsm> gsms) {
        if (gsms == null) gsms = new HashSet<>();
        if (this.gsms == null) this.gsms = new ArrayList<>(gsms.size());
        String gsmsSerialized = serializer.serialize(gsms);
        setField(StandardAttribute.GSMS, gsmsSerialized);
    }

    /**
     * Gets user's emails.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     */
    public List<String> getEmails() {
        if (this.emails == null || this.emails.isEmpty()) {
            HashSet<UserBody.Email> emailsWithPreferred = getEmailsWithPreferred();
            if (emailsWithPreferred != null) {
                for (UserBody.Email email : emailsWithPreferred) {
                    this.emails.add(email.getAddress());
                    Boolean preferredEmail = email.getPreferred();
                    if (preferredEmail != null && preferredEmail)
                        this.preferredEmail = email.getAddress();
                }
            }
        }
        return this.emails;
    }

    /**
     * Sets user's emails.
     * You can provide additional user's information to the server, so that you will be able to send personalised targeted messages to the exact user.
     *
     * @see MobileMessaging#saveUser(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUser(UserData)
     */
    public void setEmails(List<String> emails) {
        if (emails == null) emails = Collections.emptyList();
        this.emails = emails;

        HashSet<UserBody.Email> emailsWithPreferred = getEmailsWithPreferred();
        if (emailsWithPreferred == null) emailsWithPreferred = new HashSet<>();
        for (String email : emails) {
            if (TextUtils.isEmpty(email)) continue;
            emailsWithPreferred.add(new UserBody.Email(email, email.equals(preferredEmail)));
        }
        setEmailsWithPreferred(emailsWithPreferred);
    }

    protected HashSet<UserBody.Email> getEmailsWithPreferred() {
        Type type = new TypeToken<Set<UserBody.Email>>() {}.getType();
        return serializer.deserialize((String) getField(StandardAttribute.EMAILS), type);
    }

    protected void setEmailsWithPreferred(Set<UserBody.Email> emails) {
        if (emails == null) emails = new HashSet<>();
        if (this.emails == null) this.emails = new ArrayList<>(emails.size());
        String emailsSerialized = serializer.serialize(emails);
        setField(StandardAttribute.EMAILS, emailsSerialized);
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
     * @see MobileMessaging#saveUser(UserData, MobileMessaging.ResultListener)
     * @see MobileMessaging#saveUser(UserData)
     */
    public void setTags(Set<String> tags) {
        if (tags == null) tags = new HashSet<>(0);
        this.tags = tags;
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
        EXTERNAL_USER_ID("externalUserId");

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
