package org.infobip.mobile.messaging.chat.core

/**
 * Provides JSON Web Token (JWT), to give livechat widget ability to authenticate.
 */
fun interface JwtProvider {

    /**
     * Provides JSON Web Token (JWT), to give livechat widget ability to authenticate.
     * Function can be triggered multiple times during widget lifetime, due to various events like screen orientation change, internet re-connection.
     * If you can ensure JWT expiration time is more than widget lifetime, you can return cached token, otherwise
     * **it is important to provide fresh new token for each invocation.**
     *
     * @return JWT
     */
    fun provideJwt(): String?

}