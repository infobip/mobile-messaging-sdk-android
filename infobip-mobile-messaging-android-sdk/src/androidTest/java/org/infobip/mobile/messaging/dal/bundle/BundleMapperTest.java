/*
 * BundleMapperTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.dal.bundle;

import junit.framework.TestCase;

import org.json.JSONArray;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author sslavin
 * @since 13/09/2017.
 */

public class BundleMapperTest extends TestCase {

    public void test_shouldSerializeDeserializeJSONObject() throws Exception {

        // Given
        JSONObject givenObject = new JSONObject().put("someObject", new JSONObject().put("someValue", "value"));

        // When
        JSONObject actualObject = BundleMapper.objectFromBundle(BundleMapper.objectToBundle(givenObject, "someTag"), "someTag", JSONObject.class);

        // Then
        JSONAssert.assertEquals(givenObject, actualObject, true);
    }

    public void test_shouldSerializeDeserializeJSONArray() throws Exception {

        // Given
        JSONArray givenArray = new JSONArray().put(new JSONArray().put("someValue"));

        // When
        JSONArray actualArray = BundleMapper.objectFromBundle(BundleMapper.objectToBundle(givenArray, "someTag"), "someTag", JSONArray.class);

        // Then
        JSONAssert.assertEquals(givenArray, actualArray, true);
    }

}
