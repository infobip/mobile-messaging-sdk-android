package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.shaded.google.gson.Gson;
import org.infobip.mobile.messaging.api.shaded.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author sslavin
 * @since 15/07/16.
 */
public class UserData {
    private Map<String, Object> predefinedUserData;
    private Map<String, Object> customUserData;

    public UserData() {
        this.predefinedUserData = new HashMap<>();
        this.customUserData = new HashMap<>();
    }

    public UserData(String userData) {
        Gson gson = new Gson();
        UserData data = gson.fromJson(userData, UserData.class);
        this.predefinedUserData = data.predefinedUserData;
        this.customUserData = data.customUserData;
    }

    public UserData(Map<String, Object> predefinedUserData, Map<String, Object> customUserData) {
        this.predefinedUserData = predefinedUserData;
        this.customUserData = customUserData;
    }

    public static UserData merge(UserData old, UserData latest) {
        if (old == null) {
            return latest;
        }
        if (latest == null) {
            return old;
        }

        UserData merged = new UserData();
        for (PredefinedField field : PredefinedField.values()) {
            merged.mergeField(field, old, latest);
        }
        merged.customUserData.putAll(old.customUserData);
        merged.customUserData.putAll(latest.customUserData);
        return merged;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }

    public Map<String, Object> getPredefinedUserData() {
        return predefinedUserData;
    }

    public void setPredefinedUserData(Map<String, Object> predefinedUserData) {
        this.predefinedUserData = customUserData;
    }

    public Map<String, Object> getCustomUserData() {
        return customUserData;
    }

    public void setCustomUserData(Map<String, Object> customUserData) {
        this.customUserData = customUserData;
    }

    public String getMsisdn() {
        return getField(PredefinedField.MSISDN);
    }

    public void setMsisdn(String msisdn) {
        setField(PredefinedField.MSISDN, msisdn);
    }

    public String getFirstName() {
        return getField(PredefinedField.FIRST_NAME);
    }

    public void setFirstName(String firstName) {
        setField(PredefinedField.FIRST_NAME, firstName);
    }

    public String getLastName() {
        return getField(PredefinedField.LAST_NAME);
    }

    public void setLastName(String lastName) {
        setField(PredefinedField.LAST_NAME, lastName);
    }

    public String getGender() {
        return getField(PredefinedField.GENDER);
    }

    public void setGender(String gender) {
        setField(PredefinedField.GENDER, gender);
    }

    public String getBirthdate() {
        return getField(PredefinedField.BIRTHDATE);
    }

    public void setBirthdate(String birthdate) {
        setField(PredefinedField.BIRTHDATE, birthdate);
    }

    public String getEmail() {
        return getField(PredefinedField.EMAIL);
    }

    public void setEmail(String email) {
        setField(PredefinedField.EMAIL, email);
    }

    private <T> T getField(PredefinedField field) {
        Object object = predefinedUserData.get(field.getKey());
        try {
            return (T) object;
        } catch (ClassCastException e) {
            return null;
        }
    }

    private void mergeField(PredefinedField field, UserData oldData, UserData newData) {
        if (newData.predefinedUserData.containsKey(field.getKey())) {
            this.predefinedUserData.put(field.getKey(), newData.predefinedUserData.get(field.getKey()));
        } else if (oldData.predefinedUserData.containsKey(field.getKey())){
            this.predefinedUserData.put(field.getKey(), oldData.predefinedUserData.get(field.getKey()));
        }
    }

    private void setField(PredefinedField field, Object value) {
        predefinedUserData.put(field.getKey(), value);
    }

    protected enum PredefinedField {
        MSISDN("msisdn"),
        FIRST_NAME("firstName"),
        LAST_NAME("lastName"),
        GENDER("gender"),
        BIRTHDATE("birthdate"),
        EMAIL("email");

        private final String key;

        PredefinedField(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
