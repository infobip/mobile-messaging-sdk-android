package org.infobip.mobile.messaging.util;

import android.util.Base64;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

public class AuthorizationUtils {
    private static final Set<String> mandatoryHeaders = Set.of("alg", "typ", "kid");
    private static final Set<String> mandatoryClaims = Set.of("typ", "sub", "infobip-api-key", "iat", "exp", "jti");
    private static final String TOKEN_IS_NULL = "Token is null.";
    private static final String TOKEN_IS_EMPTY_OR_BLANK = "Token is empty or blank.";
    private static final String TOKEN_PARTS_NUMBER = "Token must have three parts separated by dots.";
    private static final String HEADER_NOT_VALID_JSON = "Token header is not a valid Base64 encoded JSON object.";
    private static final String PAYLOAD_NOT_VALID_JSON = "Token payload is not a valid Base64 encoded JSON object.";

    private static String HEADER_MISSING(String missingHeader) {
        return "Token header is missing mandatory header " + missingHeader;
    }

    private static String PAYLOAD_MISSING_CLAIM(String missingClaim) {
        return "Token payload is missing mandatory claim " + missingClaim;
    }

    static void isValidJwtStructure(String token) throws JwtStructureValidationException {
        if (token == null)
            throw new JwtStructureValidationException(TOKEN_IS_NULL);
        if (token.isBlank())
            throw new JwtStructureValidationException(TOKEN_IS_EMPTY_OR_BLANK);

        String[] jwtParts = token.split("\\.");
        if (jwtParts.length != 3)
            throw new JwtStructureValidationException(TOKEN_PARTS_NUMBER);

        JSONObject headerJson = decodeAndParse(jwtParts[0]);
        if (headerJson == null)
            throw new JwtStructureValidationException(HEADER_NOT_VALID_JSON);
        for (String header : mandatoryHeaders) {
            if (!headerJson.has(header))
                throw new JwtStructureValidationException(HEADER_MISSING(header));
        }

        JSONObject payloadJson = decodeAndParse(jwtParts[1]);
        if (payloadJson == null)
            throw new JwtStructureValidationException(PAYLOAD_NOT_VALID_JSON);
        for (String claim : mandatoryClaims) {
            if (!payloadJson.has(claim))
                throw new JwtStructureValidationException(PAYLOAD_MISSING_CLAIM(claim));
        }
    }

    static boolean isTokenExpired(String token) {
        isValidJwtStructure(token);
        String[] jwtParts = token.split("\\.");
        JSONObject payloadJson = decodeAndParse(jwtParts[1]);
        if (payloadJson == null)
            return true;
        try {
            long exp = payloadJson.getLong("exp");
            long currentTimeSeconds = System.currentTimeMillis() / 1000L;
            if (currentTimeSeconds >= exp)
                return true;
        } catch (JSONException e) {
            return true;
        }
        return false;
    }

    private static JSONObject decodeAndParse(String encoded) {
        if (encoded == null)
            return null;
        try {
            String headerDecoded = new String(Base64.decode(encoded, Base64.URL_SAFE | Base64.NO_WRAP));
            return new JSONObject(headerDecoded);
        } catch (IllegalArgumentException | JSONException e) {
            return null;
        }
    }

    public static String authorizationHeader(MobileMessagingCore mobileMessagingCore, Broadcaster broadcaster) {
        String token = mobileMessagingCore.getJwtFromSupplier();
        if (token != null && isTokenExpired(token)) {
            broadcaster.userDataJwtExpired();
            throw new JwtExpirationException();
        }
        return token != null ? "Bearer " + token : "App " + mobileMessagingCore.getApplicationCode();
    }
}
