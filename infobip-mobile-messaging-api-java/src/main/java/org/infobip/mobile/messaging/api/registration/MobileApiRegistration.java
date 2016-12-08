package org.infobip.mobile.messaging.api.registration;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Mobile registration update/sync API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiRegistration mobileApiRegistration = new Generator.Builder().build().create(MobileApiRegistration.class);
 * }</pre>
 *
 * @author mstipanov
 * @see Generator
 * @see Generator.Builder
 * @since 17.03.2016.
 */
@Version("2")
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}")
public interface MobileApiRegistration {

    /**
     * Updates or creates the cloud registration token on the Mobile Messaging backend system.
     *
     * @param deviceApplicationInstanceId null if it doesn't exist
     * @param registrationId              new registration token
     * @return {@link RegistrationResponse}
     */
    @HttpRequest(method = HttpMethod.POST, value = "registration")
    @Query(name = "platformType", value = "${platform.type:GCM}")
    RegistrationResponse upsert(@Query(name = "deviceApplicationInstanceId") String deviceApplicationInstanceId,
                                @Query(name = "registrationId") String registrationId,
                                @Query(name = "pushRegistrationEnabled") Boolean pushRegistrationEnabled);
}
