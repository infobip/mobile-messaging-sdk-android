package org.infobip.mobile.messaging.util;

import android.util.Base64;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
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

    static void isValidJwt(String token) throws JwtStructureValidationException, JwtExpirationException {
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

        try {
            long exp = payloadJson.getLong("exp");
            long currentTimeSeconds = System.currentTimeMillis() / 1000L;
            if (currentTimeSeconds >= exp)
                throw new JwtExpirationException();
        } catch (JSONException e) {
            throw new JwtStructureValidationException(PAYLOAD_MISSING_CLAIM("exp"));
        }
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

    private static String authorizationHeader(String token, String applicationCode) {
        return token != null ? "Bearer " + token : "App " + applicationCode;
    }

    /**
     * Create the authorization header for API request. Will first
     * get JWT token from {@code JwtSupplier} and validate it. If the token
     * is not valid because of wrong structure or if it expired,
     * will send error result to the {@code listener} and return {@code null}.
     * If there is no {@code JwtSupplier} or the token from it is {@code null}
     * the authorization header will be with application code.
     * @return authorization header with either token or application code, or {@code null} if there was an error
     */
    public static String getAuthorizationHeader(MobileMessagingCore mobileMessagingCore, MobileMessaging.ResultListener listener) {
        String token = mobileMessagingCore.getJwtFromSupplier();
        if (token != null) {
            try {
                isValidJwt(token);
            } catch (JwtStructureValidationException | JwtExpirationException e) {
                MobileMessagingLogger.e("JWT token structure is invalid or the token is expired.", e);
                if (listener != null) {
                    listener.onResult(new Result<>(mobileMessagingCore.getUser(), MobileMessagingError.createFrom(e)));
                }
                return null;
            }
        }
        return authorizationHeader(token, mobileMessagingCore.getApplicationCode());
    }
}
