package org.infobip.mobile.messaging.api.rtc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TokenBody {
    private final String identity;
    private final String applicationId;
    private final Long timeToLive;
}
