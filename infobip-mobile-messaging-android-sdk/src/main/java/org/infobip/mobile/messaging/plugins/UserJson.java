package org.infobip.mobile.messaging.plugins;

import com.google.gson.reflect.TypeToken;

import org.infobip.mobile.messaging.CustomAttributesMapper;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserAttributes;
import org.infobip.mobile.messaging.UserIdentity;
import org.infobip.mobile.messaging.UserMapper;
import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.DateTimeUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;

import static org.infobip.mobile.messaging.plugins.InstallationJson.cleanupJsonMapForClient;

/**
 * User data mapper for JSON conversion in plugins
 */
public class UserJson extends User {

    public static JSONObject toJSON(final User user) {
        if (user == null) {
            return new JSONObject();
        }
        try {
            JSONObject jsonObject = new JSONObject(UserMapper.toJson(user));
            cleanupJsonMapForClient(user.getCustomAttributes(), jsonObject);
            return jsonObject;
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    @NonNull
    public static User resolveUser(JSONObject args) throws IllegalArgumentException {
        if (args == null) {
            throw new IllegalArgumentException("Cannot resolve user from arguments");
        }

        return UserJson.fromJSON(args);
    }

    private static User fromJSON(JSONObject json) throws IllegalArgumentException {
        User user = new User();

        try {
            if (json.has(UserAtts.externalUserId)) {
                user.setExternalUserId(json.optString(UserAtts.externalUserId));
            }
            if (json.has(UserAtts.firstName)) {
                user.setFirstName(json.optString(UserAtts.firstName));
            }
            if (json.has(UserAtts.lastName)) {
                user.setLastName(json.optString(UserAtts.lastName));
            }
            if (json.has(UserAtts.middleName)) {
                user.setMiddleName(json.optString(UserAtts.middleName));
            }
            if (json.has(UserAtts.gender)) {
                user.setGender(UserMapper.genderFromBackend(json.optString(UserAtts.gender)));
            }
            if (json.has(UserAtts.birthday)) {
                Date bday = null;
                try {
                    bday = DateTimeUtil.dateFromYMDString(json.optString(UserAtts.birthday));
                    user.setBirthday(bday);
                } catch (ParseException e) {
                }
            }
            if (json.has(UserAtts.phones)) {
                user.setPhones(jsonArrayFromJSONObjectToSet(json, UserAtts.phones));
            }
            if (json.has(UserAtts.emails)) {
                user.setEmails(jsonArrayFromJSONObjectToSet(json, UserAtts.emails));
            }
            if (json.has(UserAtts.tags)) {
                user.setTags(jsonArrayFromJSONObjectToSet(json, UserAtts.tags));
            }
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot parse user fromJSON: ", e);
        }

        try {
            if (json.has(UserAtts.customAttributes)) {
                java.lang.reflect.Type type = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> customAttributes = new JsonSerializer().deserialize(json.optString(UserAtts.customAttributes), type);
                if (!CustomAttributesMapper.validate(customAttributes)) {
                    throw new IllegalArgumentException("Custom attributes are invalid.");
                }
                user.setCustomAttributes(CustomAttributesMapper.customAttsFromBackend(customAttributes));
            }
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot parse user fromJSON custom attributes: ", e);
        }

        return user;
    }

    public static UserAttributes userAttributesFromJSON(JSONObject json) throws IllegalArgumentException {
        if (json == null) {
            return null;
        }

        UserAttributes userAttributes = new UserAttributes();

        try {
            if (json.has(UserAtts.firstName)) {
                userAttributes.setFirstName(json.optString(UserAtts.firstName));
            }
            if (json.has(UserAtts.lastName)) {
                userAttributes.setLastName(json.optString(UserAtts.lastName));
            }
            if (json.has(UserAtts.middleName)) {
                userAttributes.setMiddleName(json.optString(UserAtts.middleName));
            }
            if (json.has(UserAtts.gender)) {
                userAttributes.setGender(UserMapper.genderFromBackend(json.optString(UserAtts.gender)));
            }
            if (json.has(UserAtts.birthday)) {
                Date bday = null;
                try {
                    bday = DateTimeUtil.dateFromYMDString(json.optString(UserAtts.birthday));
                    userAttributes.setBirthday(bday);
                } catch (ParseException e) {
                    MobileMessagingLogger.e("Cannot parse user birthday: ", e);
                }
            }
            if (json.has(UserAtts.tags)) {
                userAttributes.setTags(jsonArrayFromJSONObjectToSet(json, UserAtts.tags));
            }
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot parse user attributes: ", e);
        }

        try {
            if (json.has(UserAtts.customAttributes)) {
                java.lang.reflect.Type type = new TypeToken<Map<String, Object>>() {
                }.getType();
                Map<String, Object> customAttributes = new JsonSerializer().deserialize(json.optString(UserAtts.customAttributes), type);
                if (!CustomAttributesMapper.validate(customAttributes)) {
                    throw new IllegalArgumentException("Custom attributes are invalid.");
                }
                userAttributes.setCustomAttributes(CustomAttributesMapper.customAttsFromBackend(customAttributes));
            }
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot parse user custom attributes: ", e);
        }

        return userAttributes;
    }

    public static UserIdentity userIdentityFromJSON(JSONObject json) {
        UserIdentity userIdentity = new UserIdentity();
        try {
            if (json.has(UserAtts.externalUserId)) {
                userIdentity.setExternalUserId(json.optString(UserAtts.externalUserId));
            }
            if (json.has(UserAtts.phones)) {
                userIdentity.setPhones(jsonArrayFromJSONObjectToSet(json, UserAtts.phones));
            }
            if (json.has(UserAtts.emails)) {
                userIdentity.setEmails(jsonArrayFromJSONObjectToSet(json, UserAtts.emails));
            }
        } catch (Exception e) {
            MobileMessagingLogger.e("Cannot parse user identity: ", e);
        }
        return userIdentity;
    }

    private static Set<String> jsonArrayFromJSONObjectToSet(JSONObject jsonObject, String arrayName) {
        Set<String> set = new HashSet<String>();
        JSONArray jsonArray = jsonObject.optJSONArray(arrayName);
        if (jsonArray != null) {
            for (int i = 0; i < jsonArray.length(); i++) {
                set.add(jsonArray.optString(i));
            }
        }
        return set;
    }
}