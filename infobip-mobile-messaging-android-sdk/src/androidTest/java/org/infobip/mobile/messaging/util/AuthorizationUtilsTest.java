package org.infobip.mobile.messaging.util;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public class AuthorizationUtilsTest {

    private final String validHeader;
    private final String validPayload;
    private final String validHeaderEncoded;
    private final String validPayloadEncoded;
    private final Base64.Encoder encoder;

    public AuthorizationUtilsTest() {
        encoder = Base64.getEncoder();
        try {
            JSONObject header = new JSONObject();
            header.put("alg", "HS256");
            header.put("typ", "JWT");
            header.put("kid", "validKID");
            validHeader = header.toString();
            validHeaderEncoded = encoder.encodeToString(validHeader.getBytes());

            JSONObject payload = new JSONObject();
            payload.put("typ", "Bearer");
            payload.put("sub", "externalUserId");
            payload.put("infobip-api-key", "myApiKey");
            payload.put("iat", Instant.now().getEpochSecond());
            payload.put("exp", Instant.now().plusSeconds(60).getEpochSecond());
            payload.put("jti", UUID.randomUUID().toString());
            validPayload = payload.toString();
            validPayloadEncoded = encoder.encodeToString(validPayload.getBytes());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void shouldFailStructureBecauseTokenIsNull() {
        JwtStructureValidationException exception = assertThrows(JwtStructureValidationException.class, () -> {
            AuthorizationUtils.isValidJwtStructure(null);
        });
        assertEquals(exception.getMessage(), "Token is null.");
    }

    @Test
    public void shouldFailStructureBecauseTokenIsBlank() {
        JwtStructureValidationException exception = assertThrows(JwtStructureValidationException.class, () -> {
            AuthorizationUtils.isValidJwtStructure("   ");
        });
        assertEquals(exception.getMessage(), "Token is empty or blank.");
    }

    @Test
    public void shouldFailStructureBecauseTokenDoesntHaveThreeParts() {
        JwtStructureValidationException exception = assertThrows(JwtStructureValidationException.class, () -> {
            AuthorizationUtils.isValidJwtStructure("header.payload");
        });
        assertEquals(exception.getMessage(), "Token must have three parts separated by dots.");
    }

    @Test
    public void shouldFailStructureBecauseHeaderIsNotBase64Encoded() {
        JwtStructureValidationException exception = assertThrows(JwtStructureValidationException.class, () -> {
            AuthorizationUtils.isValidJwtStructure(validHeader + ".payload.signature");
        });
        assertEquals(exception.getMessage(), "Token header is not a valid Base64 encoded JSON object.");
    }

    @Test
    public void shouldFailStructureBecauseHeaderIsNotValidJson() {
        String encodedHeader = encoder.encodeToString("header".getBytes());
        JwtStructureValidationException exception = assertThrows(JwtStructureValidationException.class, () -> {
            AuthorizationUtils.isValidJwtStructure(encodedHeader + ".payload.signature");
        });
        assertEquals(exception.getMessage(), "Token header is not a valid Base64 encoded JSON object.");
    }

    @Test
    public void shouldFailStructureBecausePayloadIsNotBase64Encoded() {
        JwtStructureValidationException exception = assertThrows(JwtStructureValidationException.class, () -> {
            AuthorizationUtils.isValidJwtStructure(validHeaderEncoded + "." + validPayload + ".signature");
        });
        assertEquals(exception.getMessage(), "Token payload is not a valid Base64 encoded JSON object.");
    }

    @Test
    public void shouldFailStructureBecausePayloadIsNotValidJson() {
        String encodedPayload = encoder.encodeToString("payload".getBytes());

        JwtStructureValidationException exception = assertThrows(JwtStructureValidationException.class, () -> {
            AuthorizationUtils.isValidJwtStructure(validHeaderEncoded + "." + encodedPayload + ".signature");
        });
        assertEquals(exception.getMessage(), "Token payload is not a valid Base64 encoded JSON object.");
    }

    @Test
    public void shouldFailStructureBecauseMandatoryHeaderIsMissing() throws JSONException {
        JSONObject notValidHeader = new JSONObject(validHeader);
        notValidHeader.remove("kid");
        String encodedHeader = encoder.encodeToString(notValidHeader.toString().getBytes());

        JwtStructureValidationException exception = assertThrows(JwtStructureValidationException.class, () -> {
            AuthorizationUtils.isValidJwtStructure(encodedHeader + ".payload.signature");
        });
        assertEquals(exception.getMessage(), "Token header is missing mandatory header kid");
    }

    @Test
    public void shouldFailStructureBecauseMandatoryClaimIsMissing() throws JSONException {
        JSONObject notValidPayload = new JSONObject(validPayload);
        notValidPayload.remove("infobip-api-key");
        String encodedPayload = encoder.encodeToString(notValidPayload.toString().getBytes());

        JwtStructureValidationException exception = assertThrows(JwtStructureValidationException.class, () -> {
            AuthorizationUtils.isValidJwtStructure(validHeaderEncoded + "." + encodedPayload + ".signature");
        });
        assertEquals(exception.getMessage(), "Token payload is missing mandatory claim infobip-api-key");
    }

    @Test
    public void shouldPassStructureCheck() throws JwtStructureValidationException {
        AuthorizationUtils.isValidJwtStructure(validHeaderEncoded + "." + validPayloadEncoded + ".signature");
    }

    @Test
    public void shouldReturnTokenExpired() throws JSONException {
        JSONObject expiredPayload = new JSONObject(validPayload);
        expiredPayload.put("exp", Instant.now().minusSeconds(10).getEpochSecond());
        String encodedPayload = encoder.encodeToString(expiredPayload.toString().getBytes());

        assertTrue(AuthorizationUtils.isTokenExpired(validHeaderEncoded + "." + encodedPayload + ".signature"));
    }

    @Test
    public void shouldNotReturnTokenExpired() {
        assertFalse(AuthorizationUtils.isTokenExpired(validHeaderEncoded + "." + validPayloadEncoded + ".signature"));
    }

}
