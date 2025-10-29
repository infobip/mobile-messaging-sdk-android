package org.infobip.mobile.messaging.api.clickreporter;

import org.infobip.mobile.messaging.api.support.http.FullUrl;
import org.infobip.mobile.messaging.api.support.http.Header;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

public interface MobileApiClickReporter {
    @HttpRequest(method = HttpMethod.GET, value = "")
    void get(@FullUrl String fullUrl,
             @Header(name = "Authorization") String authorization,
             @Header(name = "pushRegistrationId") String pushRegistrationId,
             @Header(name = "buttonidx") String buttonIdx,
             @Header(name = "User-Agent") String userAgent);
}
