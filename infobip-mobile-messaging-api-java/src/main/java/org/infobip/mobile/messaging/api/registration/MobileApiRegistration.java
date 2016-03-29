package org.infobip.mobile.messaging.api.registration;

import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * @author mstipanov
 * @since 17.03.2016.
 */
public interface MobileApiRegistration {

    @HttpRequest(method = HttpMethod.POST, value = "mobile/{version}/registration")
    @Query(name = "platformType", value = "${platform.type:GCM}")
    RegistrationResponse create(@Query(name = "registrationId") String registrationId);

    @HttpRequest(method = HttpMethod.PUT, value = "/mobile/{version}/registration")
    RegistrationResponse update(@Query(name = "deviceApplicationInstanceId") String deviceApplicationInstanceId, @Query(name = "registrationId") String registrationId);
}
