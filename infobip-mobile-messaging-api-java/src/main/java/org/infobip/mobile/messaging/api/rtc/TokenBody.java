package org.infobip.mobile.messaging.api.rtc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TokenBody {
    private final String identity;
    private final String applicationId;
    private final Long timeToLive;

    @Override
    public String toString() {
        return "TokenBody{" +
                "identity='" + identity + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", timeToLive=" + timeToLive +
                '}';
    }
}
