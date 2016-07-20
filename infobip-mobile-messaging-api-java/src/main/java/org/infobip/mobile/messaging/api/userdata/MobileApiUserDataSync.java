package org.infobip.mobile.messaging.api.userdata;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * User data synchronization API.
 *
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiUserDataSync mobileApiUserDataSync = new Generator.Builder().build().create(MobileApiUserDataSync.class);
 * }</pre>
 *
 * @author sslavin
 * @see Generator
 * @see Generator.Builder
 * @since 15/07/16.
 */
@Version("1")
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}")
public interface MobileApiUserDataSync {

    @HttpRequest(method = HttpMethod.POST, value = "userdata")
    UserDataReport sync(@Query(name = "deviceApplicationInstanceId") String deviceApplicationInstanceId, @Query(name = "externalUserId") String externalUserId, @Body UserDataReport userDataReport);
}
