package org.infobip.mobile.messaging.api.data;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Data synchronization API.
 *
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiData mobileApiData = new Generator.Builder().build().create(MobileApiData.class);
 * }</pre>
 *
 * @author sslavin
 * @see Generator
 * @see Generator.Builder
 * @since 25/08/16.
 */
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}/data")
public interface MobileApiData {

    @Version("1")
    @HttpRequest(method = HttpMethod.POST, value = "system")
    void reportSystemData(@Query(name = "deviceApplicationInstanceId") String deviceApplicationInstanceId,
                          @Body SystemDataReport systemDataReport);

    @Version("3")
    @HttpRequest(method = HttpMethod.POST, value = "user")
    UserDataReport reportUserData(@Query(name = "deviceApplicationInstanceId") String deviceApplicationInstanceId,
                                  @Query(name = "externalUserId") String externalUserId,
                                  @Body org.infobip.mobile.messaging.api.data.UserDataReport userDataReport);
}
