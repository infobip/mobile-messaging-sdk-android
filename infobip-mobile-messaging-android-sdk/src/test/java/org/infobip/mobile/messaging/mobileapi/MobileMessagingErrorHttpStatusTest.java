/*
 * MobileMessagingErrorHttpStatusTest.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.mobileapi;

import org.infobip.mobile.messaging.api.support.ApiBackendException;
import org.infobip.mobile.messaging.api.support.ApiBackendExceptionWithContent;
import org.infobip.mobile.messaging.api.support.ApiException;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendCommunicationException;
import org.infobip.mobile.messaging.mobileapi.common.exceptions.BackendInvalidParameterException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MobileMessagingErrorHttpStatusTest {
    @Test
    public void mapsServiceErrorWithoutReplacingCodeOrMessage() {
        MobileMessagingError error = MobileMessagingError.createFrom(
                new ApiException("INVALID_CURSOR", "Cursor does not match filters", null, 400));

        assertEquals("INVALID_CURSOR", error.getCode());
        assertEquals("Cursor does not match filters", error.getMessage());
        assertEquals(Integer.valueOf(400), error.getHttpStatusCode());
        assertEquals(MobileMessagingError.Type.SERVER_ERROR, error.getType());
    }

    @Test
    public void preservesStatusThroughCommunicationErrorWrapper() {
        ApiIOException cause = new ApiBackendException("-2", "HTTP 502: Unknown API backend error", null, 502);
        MobileMessagingError error = MobileMessagingError.createFrom(
                new BackendCommunicationException(cause.getMessage(), cause));

        assertEquals("-2", error.getCode());
        assertEquals(cause.getMessage(), error.getMessage());
        assertEquals(Integer.valueOf(502), error.getHttpStatusCode());
        assertEquals(MobileMessagingError.Type.SERVER_ERROR, error.getType());
    }

    @Test
    public void preservesStatusThroughInvalidParameterWrapper() {
        ApiIOException cause = new ApiException("REQUEST_FORMAT_INVALID", "Invalid request", null, 400);
        MobileMessagingError error = MobileMessagingError.createFrom(
                new BackendInvalidParameterException(cause.getMessage(), cause));

        assertEquals("REQUEST_FORMAT_INVALID", error.getCode());
        assertEquals(Integer.valueOf(400), error.getHttpStatusCode());
    }

    @Test
    public void localValidationHasNoHttpStatus() {
        MobileMessagingError error = InternalSdkError.INBOX_INVALID_PARAMETERS.getError("JWT token must not be blank");
        assertEquals("20013", error.getCode());
        assertEquals(MobileMessagingError.Type.SDK_ERROR, error.getType());
        assertNull(error.getHttpStatusCode());
    }

    @Test
    public void legacyExceptionConstructorsRemainUsableWithoutStatus() {
        Throwable cause = new IllegalStateException("cause");
        ApiIOException[] exceptions = {
                new ApiIOException("code", "message"),
                new ApiIOException("code", "message", cause),
                new ApiException("code", "message"),
                new ApiException("code", "message", cause),
                new ApiBackendException("code", "message"),
                new ApiBackendException("code", "message", cause),
                new ApiBackendExceptionWithContent("code", "message", new Object())
        };
        for (ApiIOException exception : exceptions) {
            assertNull(exception.getHttpStatusCode());
            MobileMessagingError error = MobileMessagingError.createFrom(exception);
            assertEquals("code", error.getCode());
            assertEquals("message", error.getMessage());
            assertNull(error.getHttpStatusCode());
        }
    }

    @Test
    public void networkAndUnknownFailuresHaveNoInventedHttpStatus() {
        MobileMessagingError networkError = MobileMessagingError.createFrom(
                new ApiIOException("-4", "Connection failed", new java.io.IOException()));
        assertEquals("-4", networkError.getCode());
        assertNull(networkError.getHttpStatusCode());
        assertNull(MobileMessagingError.createFrom(new IllegalStateException("failure")).getHttpStatusCode());
    }

    @Test
    public void errorWithContentPreservesSuccessfulHttpStatus() {
        MobileMessagingError error = MobileMessagingError.createFrom(
                new ApiBackendExceptionWithContent("PARTIAL_ERROR", "Partial error", new Object(), 200));
        assertEquals("PARTIAL_ERROR", error.getCode());
        assertEquals(Integer.valueOf(200), error.getHttpStatusCode());
    }
}
