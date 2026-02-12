/*
 * UserJsonTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserAttributes;
import org.infobip.mobile.messaging.UserIdentity;
import org.infobip.mobile.messaging.api.appinstance.UserAtts;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class UserJsonTest {
    private final static int USER_CLASS_NUMBER_OF_PARAMS = 12;

    @Test
    public void toJSON_should_return_empty_for_null() {
        JSONObject json = UserJson.toJSON(null);

        assertEquals(0, json.length());
    }

    @Test
    public void toJSON_should_be_expected_number_of_parameters() {
        User user = user();
        JSONObject json = UserJson.toJSON(user);

        assertEquals(USER_CLASS_NUMBER_OF_PARAMS, json.length());
    }

    @Test
    public void resolveUser_should_not_have_installations_and_type() {
        JSONObject json = UserJson.toJSON(user());

        User user = UserJson.resolveUser(json);

        assertNull(user.getInstallations());
        assertNull(user.getType());
    }

    @Test
    public void resolveUser_should_handle_nulls_and_ignore_type_and_installations() throws JSONException {
        JSONObject json = new JSONObject("{\"externalUserId\":null,\"firstName\":null,\"lastName\":null,\"middleName\":null,\"gender\":null,\"birthday\":null,\"type\":\"CUSTOMER\",\"phones\":null,\"emails\":null,\"tags\":[],\"customAttributes\":null,\"installations\":[]}");

        User user = UserJson.resolveUser(json);

        assertTrue(user.containsField(UserAtts.externalUserId));
        assertNull(user.getExternalUserId());
        assertTrue(user.containsField(UserAtts.firstName));
        assertNull(user.getFirstName());
        assertTrue(user.containsField(UserAtts.lastName));
        assertNull(user.getLastName());
        assertTrue(user.containsField(UserAtts.middleName));
        assertNull(user.getMiddleName());
        assertTrue(user.containsField(UserAtts.birthday));
        assertNull(user.getBirthday());
        assertTrue(user.containsField(UserAtts.gender));
        assertNull(user.getGender());
        assertTrue(user.containsField(UserAtts.tags));
        assertNull(user.getTags());
        assertTrue(user.containsField(UserAtts.phones));
        assertNull(user.getPhones());
        assertTrue(user.containsField(UserAtts.emails));
        assertNull(user.getEmails());
        assertTrue(user.containsField(UserAtts.customAttributes));
        assertNull(user.getCustomAttributes());

        assertFalse(user.containsField("type"));
        assertFalse(user.containsField("installations"));
    }

    @Test
    public void resolveUser_should_handle_not_filled() throws JSONException {
        JSONObject json = new JSONObject("{\"middleName\":\"Justin\"}");

        User user = UserJson.resolveUser(json);

        assertEquals(1, user.getMap().size());
        assertTrue(user.containsField(UserAtts.middleName));
        assertEquals("Justin", user.getMiddleName());
    }

    @Test
    public void userIdentityFromJSON_should_resolve_arrays_correctly() throws JSONException {
        JSONObject json = new JSONObject("{\"externalUserId\":\"someExternalUserId\",\"phones\":[\"123456789\",\"987654321\"],\"emails\":[\"some@email.com\"]}");

        UserIdentity userIdentity = UserJson.userIdentityFromJSON(json);

        assertEquals("someExternalUserId", userIdentity.getExternalUserId());
        assertEquals(2, userIdentity.getPhones().size());
        assertEquals(1, userIdentity.getEmails().size());
    }

    @Test
    public void userIdentityFromJSON_should_ignore_empty_arrays() throws JSONException {
        JSONObject json = new JSONObject("{\"externalUserId\":\"someExternalUserId\",\"phones\":[],\"emails\":[\"\"]}");

        UserIdentity userIdentity = UserJson.userIdentityFromJSON(json);

        assertEquals("someExternalUserId", userIdentity.getExternalUserId());
        assertNull(userIdentity.getEmails());
        assertNull(userIdentity.getPhones());
    }

    @Test
    public void userAttributesFromJSON_should_nullify_only_provided_params() throws JSONException {
        JSONObject json = new JSONObject("{\"firstName\":null,\"lastName\":null,\"gender\":null,\"birthday\":null,\"tags\":[]}");

        UserAttributes userAttributes = UserJson.userAttributesFromJSON(json);

        assertTrue(userAttributes.containsField(UserAtts.firstName));
        assertNull(userAttributes.getFirstName());
        assertTrue(userAttributes.containsField(UserAtts.lastName));
        assertNull(userAttributes.getLastName());
        assertTrue(userAttributes.containsField(UserAtts.gender));
        assertNull(userAttributes.getGender());
        assertTrue(userAttributes.containsField(UserAtts.birthday));
        assertNull(userAttributes.getBirthday());
        assertTrue(userAttributes.containsField(UserAtts.tags));

        assertFalse(userAttributes.containsField(UserAtts.middleName));
    }

    @Test
    public void userIdentity_ignores_empty_sets() {
        UserIdentity userIdentity = new UserIdentity();
        HashSet<String> emails = new HashSet<>();

        userIdentity.setEmails(emails);
        userIdentity.setExternalUserId("some-id");

        assertTrue(userIdentity.containsField(UserAtts.externalUserId));
        assertNull(userIdentity.getMap().get(UserAtts.emails));
    }

    private User user() {
        List<Installation> installations = new ArrayList<>();
        installations.add(new Installation("somepushregid"));
        return new User(
                "externalUserId",
                "Jon",
                "J",
                "Doe",
                UserAttributes.Gender.Male,
                "2001-02-14",
                User.Type.CUSTOMER,
                CollectionUtils.setOf("385991111666"),
                CollectionUtils.setOf("someemail@infobip.com"),
                CollectionUtils.setOf("first", "second"),
                installations,
                null
        );
    }
}
