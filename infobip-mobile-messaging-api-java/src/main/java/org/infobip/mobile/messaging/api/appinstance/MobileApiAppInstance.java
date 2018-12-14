package org.infobip.mobile.messaging.api.appinstance;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Path;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * User and app instance related mobile API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiAppInstance mobileApiAppInstance = new Generator.Builder().build().create(MobileApiAppInstance.class);
 * }</pre>
 *
 * @see Generator
 * @see Generator.Builder
 */
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}/appinstance")
@Version("1")
public interface MobileApiAppInstance {

    @HttpRequest(method = HttpMethod.POST)
    @Query(name = "ri", value = "true")
    AppInstanceWithPushRegId createInstance(@Query(name = "rt", value = "false") boolean returnToken,
                                            @Body AppInstance instance);

    @HttpRequest(method = HttpMethod.PATCH, value = "{regId}")
    @Query(name = "rt", value = "false")
    Void patchInstance(@Path(name = "regId") String regId,
                       @Query(name = "ri", value = "false") boolean returnInstance,
                       @Body AppInstance instance);

    @HttpRequest(method = HttpMethod.GET, value = "{regId}")
    AppInstanceWithPushRegId getInstance(@Path(name = "regId") String regId);

    //TODO NOT USED (used in showcase) - delete?
    @HttpRequest(method = HttpMethod.DELETE, value = "{regId}")
    Void expireInstance(@Path(name = "regId") String regId);


    @HttpRequest(method = HttpMethod.PATCH, value = "{regId}/user")
    Void patchUser(@Path(name = "regId") String regId,
                   @Query(name = "ru", value = "false") boolean returnUser,
                   @Body UserBody user);

    @HttpRequest(method = HttpMethod.GET, value = "{regId}/user")
    @Query(name = "ri", value = "true")
    UserBody getUser(@Path(name = "regId") String regId);

    @HttpRequest(method = HttpMethod.POST, value = "{regId}/logout")
    Void logoutUser(@Path(name = "regId") String regId);
}
