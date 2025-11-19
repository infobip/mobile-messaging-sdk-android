/*
 * InstallationJsonTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.plugins;

import org.infobip.mobile.messaging.CustomAttributeValue;
import org.infobip.mobile.messaging.Installation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InstallationJsonTest {

    @Test
    public void array_toJSON_should_be_null_for_null() {
        assertNull(InstallationJson.toJSON((List<Installation>) null));
    }

    @Test
    public void toJSON_should_be_valid() throws JSONException {
        Installation installation = installation();
        JSONObject json = InstallationJson.toJSON(installation);

        assertEquals(18, json.length());
        assertEquals(installation.getAppVersion(), json.getString("appVersion"));
        assertTrue(json.has("applicationUserId"));
        assertEquals("null", json.getString("applicationUserId"));
        assertEquals(installation.getDeviceManufacturer(), json.getString("deviceManufacturer"));
        assertEquals(installation.getDeviceModel(), json.getString("deviceModel"));
        assertEquals(installation.getDeviceName(), json.getString("deviceName"));
        assertEquals(installation.getDeviceSecure(), json.get("deviceSecure"));
        assertEquals(installation.getDeviceTimezoneOffset(), json.getString("deviceTimezoneOffset"));
        assertEquals(installation.isPrimaryDevice(), json.getBoolean("isPrimaryDevice"));
        assertEquals(installation.isPushRegistrationEnabled(), json.getBoolean("isPushRegistrationEnabled"));
        assertEquals(installation.getLanguage(), json.getString("language"));
        assertEquals(installation.getNotificationsEnabled(), json.getBoolean("notificationsEnabled"));
        assertEquals(installation.getOs(), json.get("os"));
        assertEquals(installation.getOsVersion(), json.get("osVersion"));
        assertEquals(installation.getPushRegistrationId(), json.get("pushRegistrationId"));
        assertEquals(installation.getPushServiceToken(), json.get("pushServiceToken"));
        assertEquals(installation.getPushServiceType().toString(), json.get("pushServiceType"));
        assertEquals(installation.getSdkVersion(), json.get("sdkVersion"));
        assertTrue(json.has("customAttributes"));

        JSONObject value = json.getJSONObject("customAttributes");

        assertEquals("value", value.get("key"));
    }

    @Test
    public void toJSON_with_list() throws JSONException {
        List<Installation> installations = new ArrayList<>();
        installations.add(installation());
        installations.add(installation());
        installations.add(installation());

        JSONArray json = InstallationJson.toJSON(installations);

        assertNotNull(json);
        assertEquals(3, json.length());
        assertEquals("somePushRegId", json.getJSONObject(0).get("pushRegistrationId"));
    }

    @Test
    public void resolveInstallation_should_return_limited_installation() throws JSONException {
        JSONObject json = new JSONObject("{\"isPrimaryDevice\":false,\"isPushRegistrationEnabled\":true,\"customAttributes\":{}}");

        Installation fromJson = InstallationJson.resolveInstallation(json);
        Installation installation = installation();

        assertEquals(installation.isPushRegistrationEnabled(), fromJson.isPushRegistrationEnabled());
        assertEquals(installation.isPrimaryDevice(), fromJson.isPrimaryDevice());
        assertEquals(0, fromJson.getCustomAttributes().size());
    }

    private Installation installation() {
        Installation installation = new Installation(
                "somePushRegId",
                true,
                true,
                "1.2.3",
                "1.0",
                "Android",
                "15",
                "infobip",
                "justStarting",
                true,
                "en",
                "GMT+01:00",
                null,
                "someDeviceName",
                false,
                Installation.PushServiceType.Firebase,
                "somepushservicetoken",
                null
        );

        HashMap<String, CustomAttributeValue> customAttsValueHashMap = new HashMap<>();
        customAttsValueHashMap.put("key", new CustomAttributeValue("value"));
        installation.setCustomAttributes(customAttsValueHashMap);

        return installation;
    }
}
