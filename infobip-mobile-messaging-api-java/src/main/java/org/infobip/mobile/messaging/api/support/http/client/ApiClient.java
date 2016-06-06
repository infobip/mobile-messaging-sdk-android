package org.infobip.mobile.messaging.api.support.http.client;

import java.util.Collection;
import java.util.Map;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
public interface ApiClient {
    <B, R> R execute(HttpMethod method, String uri, String apiKey, String user, String password, Map<String, Collection<Object>> queryParams, Map<String, Collection<Object>> headers, B body, Class<R> returnType);
}
