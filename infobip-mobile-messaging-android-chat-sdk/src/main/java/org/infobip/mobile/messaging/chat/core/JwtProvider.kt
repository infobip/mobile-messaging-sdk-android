package org.infobip.mobile.messaging.chat.core

/**
 * Provides JSON Web Token (JWT), to give livechat widget ability to authenticate.
 */
fun interface JwtProvider {

    /**
     * Asynchronously provides a JSON Web Token (JWT) for livechat widget authentication.
     *
     * This method may be invoked multiple times during the widget's lifecycle due to events such as
     * screen orientation changes or network reconnections. It is essential to supply a fresh new, valid JWT
     * on each invocation.
     *
     * **Note:** This function is called on the UI thread. If obtaining the JWT involves network operations
     * or is resource-intensive, you must offload the work to a background thread and invoke the callback
     * on the appropriate thread when ready.
     *
     * @param callback The callback to deliver the JWT or report an error.
     */
    fun provideJwt(callback: JwtCallback)

    /**
     * Callback interface to provide JWT.
     */
    interface JwtCallback {

        /**
         * Called when JWT is ready to be use.
         *
         * @param jwt The JSON Web Token (JWT) string.
         */
        fun onJwtReady(jwt: String)

        /**
         * Called when there is an error retrieving the JWT.
         *
         * @param error The error that occurred while trying to retrieve the JWT.
         */
        fun onJwtError(error: Throwable)
    }

}