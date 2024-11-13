package org.infobip.mobile.messaging.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.User;
import org.infobip.mobile.messaging.UserAttributes;
import org.infobip.mobile.messaging.UserIdentity;
import org.infobip.mobile.messaging.api.support.util.CollectionUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
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
    public void resolveUser_should_not_have_installations() {
        JSONObject json = UserJson.toJSON(user());

        User user = UserJson.resolveUser(json);

        assertNull(user.getInstallations());
    }

    @Test
    public void userIdentityFromJSON_should_resolve() throws JSONException {
        JSONObject json = new JSONObject("{\"externalUserId\":\"someExternalUserId\",\"phones\":[\"123456789\",\"987654321\"],\"emails\":[\"some@email.com\"]}");

        UserIdentity userIdentity = UserJson.userIdentityFromJSON(json);

        assertEquals("someExternalUserId", userIdentity.getExternalUserId());
        assertEquals(2, userIdentity.getPhones().size());
        assertEquals(1, userIdentity.getEmails().size());
    }

    @Test
    public void userIdentityFromJSON_should_not_resolve_nulls() throws JSONException {
        JSONObject json = new JSONObject("{\"externalUserId\":\"someExternalUserId\",\"phones\":[],\"emails\":[]}");

        UserIdentity userIdentity = UserJson.userIdentityFromJSON(json);

        assertEquals("someExternalUserId", userIdentity.getExternalUserId());
        assertEquals(0, userIdentity.getEmails().size());
        assertEquals(0, userIdentity.getPhones().size());
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
