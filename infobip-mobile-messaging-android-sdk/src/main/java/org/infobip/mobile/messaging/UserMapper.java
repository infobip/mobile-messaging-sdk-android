/*
 * UserMapper.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import android.os.Bundle;
import android.util.Pair;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.Nullable;


public class UserMapper {

    private static final JsonSerializer nullSerializer = new JsonSerializer(true);

    static Pair<User, Map<String, CustomAttributeValue>> migrateToNewModels(String existingUserDataSerialized) {
        User newUser = new User();
        Map<String, CustomAttributeValue> customInstallationAtts = null;

        try {
            JSONObject userDataJsonObject = new JSONObject(existingUserDataSerialized);

            if (userDataJsonObject.has("externalUserId")) {
                if (userDataJsonObject.opt("externalUserId") != null &&
                        !"null".equals(userDataJsonObject.opt("externalUserId")))
                    newUser.setExternalUserId(userDataJsonObject.optString("externalUserId"));
            }

            if (userDataJsonObject.has("predefinedUserData")) {
                JSONObject predefinedUserData = userDataJsonObject.optJSONObject("predefinedUserData");

                if (predefinedUserData.opt("firstName") != null)
                    newUser.setFirstName(predefinedUserData.optString("firstName"));
                if (predefinedUserData.opt("middleName") != null)
                    newUser.setMiddleName(predefinedUserData.optString("middleName"));
                if (predefinedUserData.opt("lastName") != null)
                    newUser.setLastName(predefinedUserData.optString("lastName"));
                if (predefinedUserData.opt("msisdn") != null) {
                    Set<String> set = new HashSet<>();
                    set.add(predefinedUserData.optString("msisdn"));
                    newUser.setPhones(set);
                }
                if (predefinedUserData.opt("email") != null) {
                    Set<String> set = new HashSet<>();
                    set.add(predefinedUserData.optString("email"));
                    newUser.setEmails(set);
                }
                if (predefinedUserData.opt("birthdate") != null)
                    newUser.setBirthday(DateTimeUtil.dateFromYMDString(predefinedUserData.optString("birthdate")));

                Object gender = predefinedUserData.opt("gender");
                if (gender instanceof String) {
                    String genderAsString = (String) gender;
                    if ("F".equalsIgnoreCase(genderAsString) || "Female".equalsIgnoreCase(genderAsString))
                        newUser.setGender(User.Gender.Female);
                    if ("M".equalsIgnoreCase(genderAsString) || "Male".equalsIgnoreCase(genderAsString))
                        newUser.setGender(User.Gender.Male);
                }
            }

            if (userDataJsonObject.optString("customUserData") != null) {
                customInstallationAtts = CustomAttributesMapper.customAttsFrom(userDataJsonObject.optString("customUserData"));
            }

        } catch (Exception e) {
            MobileMessagingLogger.e("User data migration failed %s", e);
            newUser = null;
        }

        return new Pair<>(newUser, customInstallationAtts);
    }

    @Nullable
    public static User fromBackend(UserBody userResponseBody) {
        if (userResponseBody == null) return null;
        return new User(
                userResponseBody.getExternalUserId(),
                userResponseBody.getFirstName(),
                userResponseBody.getLastName(),
                userResponseBody.getMiddleName(),
                genderFromBackend(userResponseBody.getGender()),
                userResponseBody.getBirthday(),
                typeFromBackend(userResponseBody.getType()),
                destinationsFromBackend(userResponseBody.getPhones()),
                destinationsFromBackend(userResponseBody.getEmails()),
                userResponseBody.getTags(),
                installationsFromBackend(userResponseBody),
                CustomAttributesMapper.customAttsFromBackend(userResponseBody.getCustomAttributes()));
    }

    public static User fromJson(String userDataJson) {
        if (StringUtils.isBlank(userDataJson)) {
            return null;
        }
        return nullSerializer.deserialize(userDataJson, User.class);
    }

    public static String toJson(User user) {
        if (user == null) {
            return null;
        }
        return nullSerializer.serialize(user);
    }

    static User fromBundle(String key, Bundle bundle) {
        if (bundle == null || !bundle.containsKey(key)) {
            return null;
        }

        return fromJson(bundle.getString(key));
    }

    public static Bundle toBundle(String key, User user) {
        Bundle bundle = new Bundle();
        bundle.putString(key, toJson(user));
        return bundle;
    }

    static List<Map<String, Object>> mapPhonesToBackend(Set<String> phones) {
        return mapDestinationsToBackend(phones, UserAtts.phoneNumber);
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

    @Nullable
    static User merge(User old, User latest) {
        if (old == null && latest == null) {
            return null;
        }

        User merged = new User();
        plus(merged, old);
        plus(merged, latest);
        return merged;
    }

    private static void plus(User existing, User data) {
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
        if (data.containsField(UserAtts.phones) || data.getPhones() != null) {
            existing.setPhones(data.getPhones());
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
        if (data.getType() != null) {
            existing.setType(data.getType());
        }
    }

    public static User filterOutDeletedData(User user) {
        Map<String, CustomAttributeValue> customAtts = null;
        if (user.getCustomAttributes() != null) {
            customAtts = new HashMap<>(user.getCustomAttributes());
            for (Iterator<Map.Entry<String, CustomAttributeValue>> iterator = customAtts.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, CustomAttributeValue> entry = iterator.next();
                if (entry.getValue() == null) {
                    iterator.remove();
                }
            }
        }
        return new User(
                user.getExternalUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getMiddleName(),
                user.getGender(),
                user.getBirthdayString(),
                user.getType(),
                user.getPhones(),
                user.getEmails(),
                user.getTags(),
                user.getInstallations(),
                customAtts);
    }

    public static User.Gender genderFromBackend(String gender) {
        try {
            return gender != null ? User.Gender.valueOf(gender) : null;
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot parse gender", e);
            return null;
        }
    }

    public static User.Type typeFromBackend(String type) {
        try {
            return type != null ? User.Type.valueOf(type) : null;
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot parse user type", e);
            return null;
        }
    }

    private static Set<String> destinationsFromBackend(Set<? extends UserBody.Destination> destinations) {
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
