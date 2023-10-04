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
    suspend fun getToken(identity: String): String?
}

internal class TokenProviderImpl(
    private val rtcService: MobileApiRtc,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TokenProvider {
    private var tokenResponse: TokenResponse? = null
    private val tokenLifespan = 43200L
    private var lastUsedIdentity: String? = null

    override suspend fun getToken(identity: String): String? {
        val response = if (isNewTokenNeeded(identity)) {
            createToken(identity)
        } else {
            tokenResponse?.takeIf { offsetDateTimeFromString(it.expirationTime)?.isBefore(OffsetDateTime.now()) == true }
                    ?: createToken(identity)
        }

        return response.token
    }

    private suspend fun createToken(identity: String): TokenResponse = withContext(dispatcher) {
        rtcService.getToken(TokenBody(identity, tokenLifespan))
                .also { tokenResponse = it }
    }

    private fun isNewTokenNeeded(identity: String): Boolean = identity != lastUsedIdentity

    private fun offsetDateTimeFromString(isoString: String): OffsetDateTime? {
        return runCatching {
            OffsetDateTime.parse(isoString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }.getOrNull()
    }
}