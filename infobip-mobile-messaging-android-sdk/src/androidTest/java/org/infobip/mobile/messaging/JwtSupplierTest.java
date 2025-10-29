/*
 * JwtSupplierTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

import org.infobip.mobile.messaging.api.appinstance.UserBody;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.tools.MobileMessagingTestCase;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JwtSupplierTest extends MobileMessagingTestCase {
    private JwtSupplier jwtSupplier;
    private MobileMessaging.ResultListener<User> resultListener;
    private ArgumentCaptor<Result> resultCaptor;
    private String validHeader;
    private String validPayload;
    private String expiredPayload;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        jwtSupplier = mock(JwtSupplier.class);
        resultListener = mock(MobileMessaging.ResultListener.class);
        resultCaptor = forClass(Result.class);
        Base64.Encoder encoder = Base64.getEncoder();
        try {
            JSONObject header = new JSONObject();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            header.put("kid", "validKID");
            validHeader = encoder.encodeToString(header.toString().getBytes());

            JSONObject payload = new JSONObject();
            payload.put("typ", "Bearer");
            payload.put("sub", "externalUserId");
            payload.put("infobip-api-key", "myApiKey");
            payload.put("iat", Instant.now().getEpochSecond());
            payload.put("exp", Instant.now().plusSeconds(60).getEpochSecond());
            payload.put("jti", UUID.randomUUID().toString());
            validPayload = encoder.encodeToString(payload.toString().getBytes());
            payload.put("exp", Instant.now().minusSeconds(60).getEpochSecond());
            expiredPayload = encoder.encodeToString(payload.toString().getBytes());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void beforeEach() {
        mobileMessaging.setJwtSupplier(jwtSupplier);
    }

    @After
    public void afterEach() {
        mobileMessaging.setJwtSupplier(null);
    }

    @Test
    public void fetchUserShouldFailBecauseInvalidJwtStructure() {
        given(jwtSupplier.getJwt()).willReturn(validHeader);

        mobileMessaging.fetchUser(resultListener);

        verify(resultListener, after(300).times(1)).onResult(resultCaptor.capture());
        Result result = resultCaptor.getValue();
        MobileMessagingError error = result.getError();
        assertNotNull(error);
        assertEquals(error.getCode(), "JWT_TOKEN_STRUCTURE_INVALID");
        assertEquals(error.getMessage(), "Token must have three parts separated by dots.");
    }

    @Test
    public void fetchUserShouldFailBecauseTokenExpired() {
        given(jwtSupplier.getJwt()).willReturn(validHeader + "." + expiredPayload + ".signature");

        mobileMessaging.fetchUser(resultListener);

        verify(resultListener, after(300).times(1)).onResult(resultCaptor.capture());
        Result result = resultCaptor.getValue();
        MobileMessagingError error = result.getError();
        assertNotNull(error);
        assertEquals(error.getCode(), "JWT_TOKEN_EXPIRED");
        assertEquals(error.getMessage(), "The provided JWT is expired.");
    }

    @Test
    public void fetchUserShouldWorkWithValidJWT() {
        String validJwt = validHeader + "." + validPayload + ".signature";
        given(jwtSupplier.getJwt()).willReturn(validJwt);
        given(mobileApiUserData.getUser(anyString(), anyString())).willReturn(new UserBody());

        mobileMessaging.fetchUser(resultListener);

        verify(mobileApiUserData, after(500).times(1)).getUser(anyString(), eq("Bearer " + validJwt));
        verify(resultListener, after(300).times(1)).onResult(resultCaptor.capture());
        Result result = resultCaptor.getValue();
        Assert.assertNotNull(result.getData());
        assertTrue(result.isSuccess());
        assertNull(result.getError());
    }

    @Test
    public void shouldUseAppAuthorizationIfTokenIsNull() {
        given(jwtSupplier.getJwt()).willReturn(null);
        given(mobileApiUserData.getUser(anyString(), anyString())).willReturn(new UserBody());

        mobileMessaging.fetchUser(resultListener);

        verify(mobileApiUserData, after(500).times(1)).getUser(anyString(), eq("App " + mobileMessagingCore.getApplicationCode()));
        verify(resultListener, after(300).times(1)).onResult(resultCaptor.capture());
        Result result = resultCaptor.getValue();
        Assert.assertNotNull(result.getData());
        assertTrue(result.isSuccess());
        assertNull(result.getError());
    }
}