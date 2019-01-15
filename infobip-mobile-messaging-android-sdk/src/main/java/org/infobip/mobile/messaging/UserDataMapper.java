package org.infobip.mobile.messaging;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.google.gson.reflect.TypeToken;

import org.infobip.mobile.messaging.api.appinstance.AppInstance;
import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.infobip.mobile.messaging.api.support.util.MapUtils;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.infobip.mobile.messaging.util.StringUtils;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class UserDataMapper {

    private static final JsonSerializer nullSerializer = new JsonSerializer(true);

    static Pair<UserData, Map<String, CustomUserDataValue>> migrateToNewModels(String existingUserDataSerialized) {
        UserData newUserData = new UserData();
        Map<String, CustomUserDataValue> customInstallationAtts = null;

        try {
            JSONObject userDataJsonObject = new JSONObject(existingUserDataSerialized);

            if (userDataJsonObject.has("externalUserId")) {
                if (userDataJsonObject.opt("externalUserId") != null)
                    newUserData.setExternalUserId(userDataJsonObject.optString("externalUserId"));
            }

            if (userDataJsonObject.has("predefinedUserData")) {
                JSONObject predefinedUserData = userDataJsonObject.optJSONObject("predefinedUserData");

                if (predefinedUserData.opt("firstName") != null)
                    newUserData.setFirstName(predefinedUserData.optString("firstName"));
                if (predefinedUserData.opt("middleName") != null)
                    newUserData.setMiddleName(predefinedUserData.optString("middleName"));
                if (predefinedUserData.opt("lastName") != null)
                    newUserData.setLastName(predefinedUserData.optString("lastName"));
                if (predefinedUserData.opt("msisdn") != null) {
                    Set<String> set = new HashSet<>();
                    set.add(predefinedUserData.optString("msisdn"));
                    newUserData.setGsms(set);
                } if (predefinedUserData.opt("email") != null) {
                    Set<String> set = new HashSet<>();
                    set.add(predefinedUserData.optString("email"));
                    newUserData.setEmails(set);
                } if (predefinedUserData.opt("birthdate") != null)
                    newUserData.setBirthday(DateTimeUtil.DateFromYMDString(predefinedUserData.optString("birthdate")));

                Object gender = predefinedUserData.opt("gender");
                if (gender instanceof String) {
                    String genderAsString = (String) gender;
                    if ("F".equalsIgnoreCase(genderAsString) || "Female".equalsIgnoreCase(genderAsString))
                        newUserData.setGender(UserData.Gender.Female);
                    if ("M".equalsIgnoreCase(genderAsString) || "Male".equalsIgnoreCase(genderAsString))
                        newUserData.setGender(UserData.Gender.Male);
                }
            }

            if (userDataJsonObject.optString("customUserData") != null) {
                customInstallationAtts = customAttsFrom(userDataJsonObject.optString("customUserData"));
            }

        } catch (Exception e) {
            MobileMessagingLogger.e("User data migration failed %s", e.getMessage());
            newUserData = null;
        }

        return new Pair<>(newUserData, customInstallationAtts);
    }

    public static UserData fromBackend(UserBody userResponseBody) {
        return new UserData(
                userResponseBody.getExternalUserId(),
                userResponseBody.getFirstName(),
                userResponseBody.getLastName(),
                userResponseBody.getMiddleName(),
                genderFromBackend(userResponseBody.getGender()),
                userResponseBody.getBirthday(),
                destinationsFromBackend(userResponseBody.getGsms()),
                destinationsFromBackend(userResponseBody.getEmails()),
                userResponseBody.getTags(),
                installationsFromBackend(userResponseBody),
                customAttsFromBackend(userResponseBody.getCustomAttributes()));
    }

    static UserData fromJson(String userDataJson) {
        if (StringUtils.isBlank(userDataJson)) {
            return null;
        }
        return nullSerializer.deserialize(userDataJson, UserData.class);
    }

    public static String toJson(UserData userData) {
        if (userData == null) {
            return null;
        }
        return nullSerializer.serialize(userData);
    }

    static UserData fromBundle(String key, Bundle bundle) {
        if (bundle == null || !bundle.containsKey(key)) {
            return null;
        }

        return fromJson(bundle.getString(key));
    }

    public static Bundle toBundle(String key, UserData userData) {
        Bundle bundle = new Bundle();
        bundle.putString(key, toJson(userData));
        return bundle;
    }

    public static Map<String, CustomUserDataValue> customAttsFrom(String json) {
        Type type = new TypeToken<Map<String, CustomUserDataValue>>() {
        }.getType();
        return nullSerializer.deserialize(json, type);
    }

    static List<Map<String, Object>> mapGsmsToBackend(Set<String> gsms) {
        return mapDestinationsToBackend(gsms, UserAtts.gsmNumber);
    }

    static List<Map<String, Object>> mapEmailsToBackend(Set<String> emails) {
        return mapDestinationsToBackend(emails, UserAtts.emailAddress);
    }

    private static List<Map<String, Object>> mapDestinationsToBackend(Set<String> destinations, String addressKey) {
        if (destinations == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>(destinations.size());
        for (String destination : destinations) {
            Map<String, Object> map = new HashMap<>();
            map.put(addressKey, destination);
            list.add(map);
        }
        return list;
    }

    private static List<Installation> installationsFromBackend(UserBody userResponse) {
        if (userResponse == null || userResponse.getInstances() == null) {
            return null;
        }

        List<Installation> installations = new ArrayList<>();
        for (AppInstance instance : userResponse.getInstances()) {
            installations.add(InstallationMapper.fromBackend(instance));
        }
        return installations;
    }

    @NonNull
    static Map<String, Object> customAttsToBackend(@NonNull Map<String, CustomUserDataValue> customAttributes) {
        Map<String, Object> customAttributesToReport = new HashMap<>(customAttributes.size());
        for (Map.Entry<String, CustomUserDataValue> entry : customAttributes.entrySet()) {
            customAttributesToReport.put(entry.getKey(), customValueToBackend(entry.getValue()));
        }
        return customAttributesToReport;
    }

    static Object customValueToBackend(CustomUserDataValue value) {
        if (value == null) {
            return null;
        }

        switch (value.getType()) {
            case Date: return DateTimeUtil.DateToYMDString(value.dateValue());
            case Number: return value.numberValue();
            case String: return value.stringValue();
            case Boolean: return value.booleanValue();
            default: return null;
        }
    }

    static Map<String, CustomUserDataValue> customAttsFromBackend(Map<String, Object> customAttributes) {
        Map<String, CustomUserDataValue> customUserDataValueMap = new HashMap<>();
        if (customAttributes == null) {
            return customUserDataValueMap;
        }

        for (Map.Entry<String, Object> entry : customAttributes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                String stringValue = (String) value;

                if (isPossiblyDate(stringValue)) {
                    try {
                        Date dateValue = DateTimeUtil.DateFromYMDString(stringValue);
                        customUserDataValueMap.put(key, new CustomUserDataValue(dateValue));
                        continue;
                    } catch (ParseException ignored) {
                    }
                }
                customUserDataValueMap.put(key, new CustomUserDataValue(stringValue));

            } else if (value instanceof Number) {
                customUserDataValueMap.put(key, new CustomUserDataValue((Number) value));
            } else if (value instanceof Boolean) {
                customUserDataValueMap.put(key, new CustomUserDataValue((Boolean) value));
            }
        }

        return customUserDataValueMap;
    }

    private static boolean isPossiblyDate(String stringValue) {
        return Character.isDigit(stringValue.charAt(0)) && stringValue.length() == DateTimeUtil.DATE_YMD_FORMAT.length();
    }

    @Nullable
    static UserData merge(UserData old, UserData latest) {
        if (old == null && latest == null) {
            return null;
        }

        UserData merged = new UserData();
        plus(merged, old);
        plus(merged, latest);
        return merged;
    }

    private static void plus(UserData existing, UserData data) {
        if (data == null) {
            return;
        }

        if (data.containsField(UserAtts.externalUserId) || data.getExternalUserId() != null) {
            existing.setExternalUserId(data.getExternalUserId());
        }
        if (data.containsField(UserAtts.firstName) || data.getFirstName() != null) {
            existing.setFirstName(data.getFirstName());
        }
        if (data.containsField(UserAtts.lastName) || data.getLastName() != null) {
            existing.setLastName(data.getLastName());
        }
        if (data.containsField(UserAtts.middleName) || data.getMiddleName() != null) {
            existing.setMiddleName(data.getMiddleName());
        }
        if (data.containsField(UserAtts.gender) || data.getGender() != null) {
            existing.setGender(data.getGender());
        }
        if (data.containsField(UserAtts.birthday) || data.getBirthdayString() != null) {
            existing.setBirthdayString(data.getBirthdayString());
        }
        if (data.containsField(UserAtts.gsms) || data.getGsms() != null) {
            existing.setGsms(data.getGsms());
        }
        if (data.containsField(UserAtts.emails) || data.getEmails() != null) {
            existing.setEmails(data.getEmails());
        }
        if (data.containsField(UserAtts.tags) || data.getTags() != null) {
            existing.setTags(data.getTags());
        }
        if (data.containsField(UserAtts.customAttributes) || data.getCustomAttributes() != null && !data.getCustomAttributes().isEmpty()) {
            existing.setCustomAttributes(MapUtils.concat(existing.getCustomAttributes(), data.getCustomAttributes()));
        }
        existing.setInstallations(CollectionUtils.concat(existing.getInstallations(), data.getInstallations()));
    }

    public static UserData filterOutDeletedData(UserData userData) {
        Map<String, CustomUserDataValue> customAtts = null;
        if (userData.getCustomAttributes() != null) {
            customAtts = new HashMap<>(userData.getCustomAttributes());
            for (Iterator<Map.Entry<String, CustomUserDataValue>> iterator = customAtts.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, CustomUserDataValue> entry = iterator.next();
                if (entry.getValue() == null) {
                    iterator.remove();
                }
            }
        }
        return new UserData(
                userData.getExternalUserId(),
                userData.getFirstName(),
                userData.getLastName(),
                userData.getMiddleName(),
                userData.getGender(),
                userData.getBirthdayString(),
                userData.getGsms(),
                userData.getEmails(),
                userData.getTags(),
                userData.getInstallations(),
                customAtts);
    }

    private static UserData.Gender genderFromBackend(String gender) {
        try {
            return gender != null ? UserData.Gender.valueOf(gender) : null;
        } catch (Exception e) {
            MobileMessagingLogger.w("Cannot parse gender", e);
            return null;
        }
    }

    static Set<String> destinationsFromBackend(Set<? extends UserBody.Destination> destinations) {
        if (destinations == null) {
            return null;
        }

        Set<String> addresses = new HashSet<>(destinations.size());
        for (UserBody.Destination destination : destinations) {
            addresses.add(destination.getDestinationId());
        }
        return addresses;
    }
}
