package org.infobip.mobile.messaging;

import androidx.annotation.Nullable;

/**
 * Interface for supplying JSON Web Tokens (JWT) for API authorization.
 * <p>
 * Implementations of this interface supply JWT tokens that will be used
 * by the SDK to authorize API calls which support JWT-based authorization.
 * </p>
 */
public interface JwtSupplier {
    /**
     * Returns a JSON Web Token (JWT) for authorization.
     * <p>
     * This method is called each time the SDK makes an API call that can use JWT authorization.
     * The returned token will be checked to ensure it has a valid structure and is not expired.
     * Returning {@code null} indicates that no JWT is currently available. If {@code externalUserId}
     * is not available should also return {@code null}.
     * </p>
     *
     * @return a JWT as a {@link String}, or {@code null} if no token is available
     */
    @Nullable
    String getJwt();
}