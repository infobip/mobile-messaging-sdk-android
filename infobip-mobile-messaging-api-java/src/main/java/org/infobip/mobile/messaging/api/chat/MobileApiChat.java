package org.infobip.mobile.messaging.api.chat;

import org.infobip.mobile.messaging.api.support.Generator;
import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;

/**
 * Chat-related mobile API.
 * <p>
 * Usage:
 * <pre>{@code
 * MobileApiChat mobileApiChat = new Generator.Builder().build().create(MobileApiChat.class);
 * }</pre>
 *
 * @see Generator
 * @see Generator.Builder
 */
@ApiKey("${api.key}")
@HttpRequest("/mobile/{version}/chat")
@Version("1")
public interface MobileApiChat {

    @HttpRequest(method = HttpMethod.GET, value = "widget")
    WidgetInfo getWidgetConfiguration();
}