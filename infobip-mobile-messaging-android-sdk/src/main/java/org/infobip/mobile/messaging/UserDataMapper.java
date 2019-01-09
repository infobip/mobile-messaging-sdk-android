package org.infobip.mobile.messaging;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.infobip.mobile.messaging.api.appinstance.AppInstanceWithPushRegId;
import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.infobip.mobile.messaging.api.support.util.MapUtils;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.DateTimeUtil;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class UserDataMapper {

    public static Pair<UserData, Map<String, CustomUserDataValue>> migrateToNewModels(String existingUserDataSerialized) {
        UserData newUserData = new UserData();
        Map<String, CustomUserDataValue> customInstallationAtts = null;

        try {
            JsonSerializer serializer = new JsonSerializer(false);
            JsonObject userDataJsonObject = serializer.deserialize(existingUserDataSerialized, JsonElement.class).getAsJsonObject();

            if (userDataJsonObject.keySet().contains("externalUserId")) {
                if (userDataJsonObject.get("externalUserId") != null)
                    newUserData.setExternalUserId(userDataJsonObject.get("externalUserId").getAsString());
            }

            if (userDataJsonObject.keySet().contains("predefinedUserData")) {
                JsonObject predefinedUserData = userDataJsonObject.getAsJsonObject("predefinedUserData");

                if (predefinedUserData.get("firstName") != null)
                    newUserData.setFirstName(predefinedUserData.get("firstName").getAsString());
                if (predefinedUserData.get("middleName") != null)
                    newUserData.setMiddleName(predefinedUserData.get("middleName").getAsString());
                if (predefinedUserData.get("lastName") != null)
                    newUserData.setLastName(predefinedUserData.get("lastName").getAsString());
                if (predefinedUserData.get("msisdn") != null)
                    newUserData.setGsms(Collections.singletonList(predefinedUserData.get("msisdn").getAsString()));
                if (predefinedUserData.get("email") != null)
                    newUserData.setEmails(Collections.singletonList(predefinedUserData.get("email").getAsString()));
                if (predefinedUserData.get("birthdate") != null)
                    newUserData.setBirthday(DateTimeUtil.DateFromYMDString(predefinedUserData.get("birthdate").getAsString()));
                JsonElement gender = predefinedUserData.get("gender");
                if (gender != null) {
                    String genderAsString = gender.getAsString();
                    if ("F".equalsIgnoreCase(genderAsString) || "Female".equalsIgnoreCase(genderAsString))
                        newUserData.setGender(UserData.Gender.Female);
                    if ("M".equalsIgnoreCase(genderAsString) || "Male".equalsIgnoreCase(genderAsString))
                        newUserData.setGender(UserData.Gender.Male);
                }
            }

            if (userDataJsonObject.keySet().contains("customUserData")) {
                if (userDataJsonObject.get("customUserData") != null) {
                    Type type = new TypeToken<Map<String, CustomUserDataValue>>() {
                    }.getType();
                    customInstallationAtts = new JsonSerializer().deserialize(userDataJsonObject.get("customUserData").getAsString(), type);
                }
            }

        } catch (Exception e) {
            MobileMessagingLogger.e("User data migration failed %s", e.getMessage());
            newUserData = null;
        }

        return new Pair<>(newUserData, customInstallationAtts);
    }

    public static UserBody toUserDataBody(UserData userData) {
        UserBody userBody = new UserBody();
        userBody.setExternalUserId(userData.getExternalUserId());
        userBody.setFirstName(userData.getFirstName());
        userBody.setLastName(userData.getLastName());
        userBody.setMiddleName(userData.getMiddleName());
        userBody.setBirthday(DateTimeUtil.DateToYMDString(userData.getBirthday()));
        if (userData.getGender() != null) {
            userBody.setGender(userData.getGender().name());
        }
        userBody.setEmails(emails(userData.getEmails()));
        userBody.setGsms(gsms(userData.getGsms()));
        userBody.setTags(userData.getTags());
        if (userData.getCustomAttributes() != null) {
            userBody.setCustomAttributes(mapCustomAttsForBackendReport(userData.getCustomAttributes()));
        }

        return userBody;
    }

    public static boolean isUserBodyEmpty(UserBody userBody) {
        return userBody == null || userBody.hashCode() == new UserData().hashCode() || userBody.toString().equals("{}");
    }

    public static UserData createFrom(UserBody userResponseBody) {
        UserData userData = new UserData();

        if (userResponseBody.getExternalUserId() != null)
            userData.setExternalUserId(userResponseBody.getExternalUserId());
        if (userResponseBody.getFirstName() != null)
            userData.setFirstName(userResponseBody.getFirstName());
        if (userResponseBody.getLastName() != null)
            userData.setLastName(userResponseBody.getLastName());
        if (userResponseBody.getMiddleName() != null)
            userData.setMiddleName(userResponseBody.getMiddleName());
        if (userResponseBody.getBirthday() != null) {
            try {
                userData.setBirthday(DateTimeUtil.DateFromYMDString(userResponseBody.getBirthday()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (userResponseBody.getGender() != null)
            userData.setGender(UserData.Gender.valueOf(userResponseBody.getGender()));
        if (userResponseBody.getEmails() != null)
            userData.setEmails(destinations(userResponseBody.getEmails()));
        if (userResponseBody.getGsms() != null)
            userData.setGsms(destinations(userResponseBody.getGsms()));
        if (userResponseBody.getTags() != null)
            userData.setTags(userResponseBody.getTags());
        if (userResponseBody.getCustomAttributes() != null)
            userData.setCustomAttributes(mapCustomAttsFromBackendResponse(userResponseBody.getCustomAttributes()));
        if (userResponseBody.getInstances() != null)
            userData.setInstallations(mapInstancesToUserInstallations(userResponseBody));

        return userData;
    }

    private static List<UserData.Installation> mapInstancesToUserInstallations(UserBody userResponse) {
        List<UserData.Installation> installations = new ArrayList<>();
        for (AppInstanceWithPushRegId instance : userResponse.getInstances()) {
            installations.add(UserData.Installation.createFrom(instance));
        }
        return installations;
    }

    @NonNull
    public static Map<String, Object> mapCustomAttsForBackendReport(@NonNull Map<String, CustomUserDataValue> customAttributes) {
        Map<String, Object> customAttributesToReport = new HashMap<>(customAttributes.size());
        for (Map.Entry<String, CustomUserDataValue> entry : customAttributes.entrySet()) {
            String key = entry.getKey();
            CustomUserDataValue value = entry.getValue();

            if (value == null) {
                customAttributesToReport.put(key, null);
            } else if (value.getType() == CustomUserDataValue.Type.Date) {
                customAttributesToReport.put(entry.getKey(), DateTimeUtil.DateToYMDString(value.dateValue()));
            } else if (value.getType() == CustomUserDataValue.Type.Number) {
                customAttributesToReport.put(key, value.numberValue());
            } else if (value.getType() == CustomUserDataValue.Type.String) {
                customAttributesToReport.put(key, value.stringValue());
            } else if (value.getType() == CustomUserDataValue.Type.Boolean) {
                customAttributesToReport.put(entry.getKey(), value.booleanValue());
            }
        }
        return customAttributesToReport;
    }

    public static Map<String, CustomUserDataValue> mapCustomAttsFromBackendResponse(Map<String, Object> customAttributes) {
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

    private static Set<UserBody.Email> emails(List<String> addresses) {
        if (addresses == null) {
            return null;
        }

        Set<UserBody.Email> emails = new HashSet<>(addresses.size());
        for (String address : addresses) {
            emails.add(new UserBody.Email(address));
        }
        return emails;
    }

    private static Set<UserBody.Gsm> gsms(List<String> numbers) {
        if (numbers == null) {
            return null;
        }

        Set<UserBody.Gsm> gsms = new HashSet<>(numbers.size());
        for (String number : numbers) {
            gsms.add(new UserBody.Gsm(number));
        }
        return gsms;
    }

    static List<String> destinations(Set<? extends UserBody.Destination> destinations) {
        if (destinations == null) {
            return null;
        }

        List<String> addresses = new ArrayList<>(destinations.size());
        for (UserBody.Destination destination : destinations) {
            addresses.add(destination.getDestinationId());
        }
        return addresses;
    }

    public static UserData filterOutDeletedData(UserData userData) {
        UserData modifiedData = merge(null, userData);
        Map<String, CustomUserDataValue> customAtts = modifiedData != null ? modifiedData.getCustomAttributes() : null;
        userData.setCustomAttributes(filterOutRemovedElements(customAtts));
        Map<String, Object> standardAtts = userData.getStandardAttributes();
        userData.getStandardAttributes().clear();
        userData.getStandardAttributes().putAll(filterOutRemovedElements(standardAtts));
        return userData;
    }

    @Nullable
    public static UserData merge(UserData old, UserData latest) {
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

        Map<String, Object> standardAtts = existing.getStandardAttributes();
        existing.getStandardAttributes().clear();
        existing.getStandardAttributes().putAll(MapUtils.concatOrEmpty(standardAtts, data.getStandardAttributes()));
        existing.setCustomAttributes(MapUtils.concat(existing.getCustomAttributes(), data.getCustomAttributes()));
        existing.setInstallations(CollectionUtils.concat(existing.getInstallations(), data.getInstallations()));
    }

    private static <T> Map<String, T> filterOutRemovedElements(Map<String, T> atts) {
        if (atts == null) {
            return null;
        }

        Map<String, T> newAtts = new HashMap<>(atts);
        for (Iterator<Map.Entry<String, T>> iterator = newAtts.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<String, T> entry = iterator.next();
            if (entry.getValue() == null) {
                iterator.remove();
            }
        }
        return newAtts;
    }
}
