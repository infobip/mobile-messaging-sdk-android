package org.infobip.mobile.messaging.api.appinstance;

import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.Header;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Path;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

import java.util.Map;

@HttpRequest("/mobile/{version}/appinstance")
@Version("1")
public interface MobileApiUserData {
    @HttpRequest(method = HttpMethod.POST, value = "{regId}/personalize")
    void personalize(@Path(name = "regId") String regId,
                     @Header(name = "Authorization") String accessToken,
                     @Query(name = "forceDepersonalize", value = "false") boolean forceDepersonalize,
                     @Query(name = "keepAsLead", value = "false") boolean keepAsLead,
                     @Body UserPersonalizeBody userPersonalizeBody);

    @HttpRequest(method = HttpMethod.POST, value = "{regId}/repersonalize")
    void repersonalize(@Path(name = "regId") String regId,
                       @Header(name = "Authorization") String accessToken,
                       @Body UserPersonalizeBody userPersonalizeBody);

    @HttpRequest(method = HttpMethod.PATCH, value = "{regId}/user")
    void patchUser(@Path(name = "regId") String regId,
                   @Header(name = "Authorization") String accessToken,
                   @Body Map user);

    @HttpRequest(method = HttpMethod.GET, value = "{regId}/user")
    @Query(name = "ri", value = "true")
    UserBody getUser(@Path(name = "regId") String regId,
                     @Header(name = "Authorization") String accessToken);
}