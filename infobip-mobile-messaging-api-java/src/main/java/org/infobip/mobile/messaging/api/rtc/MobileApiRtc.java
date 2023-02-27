package org.infobip.mobile.messaging.api.rtc;

import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

@ApiKey("${api.key}")
@HttpRequest("/webrtc/1/token")
public interface MobileApiRtc {

    @HttpRequest(method = HttpMethod.POST)
    TokenResponse getToken(@Body TokenBody body);
}
