package com.infobip.webrtc

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.infobip.mobile.messaging.api.rtc.MobileApiRtc
import org.infobip.mobile.messaging.api.rtc.TokenBody
import org.infobip.mobile.messaging.api.rtc.TokenResponse
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

internal interface TokenProvider {
    suspend fun getToken(identity: String, appId: String): String?
}

internal class TokenProviderImpl(
    private val rtcService: MobileApiRtc,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TokenProvider {
    private var tokenResponse: TokenResponse? = null
    private val tokenLifespan = 43200L
    private var lastUsedIdentity: String? = null
    private var lastUsedAppId: String? = null

    override suspend fun getToken(identity: String, appId: String): String? {
        val response = if (isNewTokenNeeded(identity, appId)) {
            createToken(identity, appId)
        } else {
            tokenResponse?.takeIf { offsetDateTimeFromString(it.expirationTime)?.isBefore(OffsetDateTime.now()) == true } ?: createToken(identity, appId)
        }

        return response.token
    }

    private suspend fun createToken(identity: String, appId: String): TokenResponse = withContext(dispatcher) {
        rtcService.getToken(TokenBody(identity, appId, tokenLifespan))
            .also { tokenResponse = it }
    }

    private fun isNewTokenNeeded(identity: String, appId: String): Boolean = identity != lastUsedIdentity || appId != lastUsedAppId

    private fun offsetDateTimeFromString(isoString: String): OffsetDateTime? {
        return runCatching {
            OffsetDateTime.parse(isoString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }.getOrNull()
    }
}