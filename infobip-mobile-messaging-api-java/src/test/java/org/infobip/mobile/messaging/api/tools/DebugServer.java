package org.infobip.mobile.messaging.api.tools;

import org.infobip.mobile.messaging.api.support.util.StreamUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import fi.iki.elonen.NanoHTTPD;

public class DebugServer extends NanoHTTPD {
    private Map<String, String> queryParameters = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> responseHeaders = new HashMap<>();
    private AtomicInteger requestCount = new AtomicInteger(0);
    private Response.Status status;
    private String mimeType;
    private String txt;
    private Method requestMethod;
    private String uri;
    private String body;

    public DebugServer() {
        super(0);
    }

    @Override
    public Response serve(IHTTPSession session) {
        requestCount.incrementAndGet();

        requestMethod = session.getMethod();
        uri = session.getUri();
        queryParameters = session.getParms();
        headers = session.getHeaders();
        body = readBody(session);

        Response response = new Response(status, mimeType, txt);
        for (String key : responseHeaders.keySet()) {
            response.addHeader(key, responseHeaders.get(key));
        }

        return response;
    }

    private String readBody(IHTTPSession session) {
        if (null != headers) {
            String contentLength = headers.get("content-length");
            if (null != contentLength) {
                long length = Long.parseLong(contentLength);
                if (length > 0) {
                    try {
                        return StreamUtils.readToString(session.getInputStream(), "UTF-8", length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
/*
        try {
            return StreamUtils.readToString(session.getInputStream(), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
        return null;
    }

    public void respondWith(Response.Status status, String json) {
        this.respondWith(status, "application/json", json);
    }

    public void respondWith(Response.Status status, String mimeType, String txt) {
        this.status = status;
        this.mimeType = mimeType;
        this.txt = txt;
    }

    public void respondWith(Response.Status status, String body, Map<String, String> headers) {
        this.status = status;
        this.txt = body;
        this.mimeType = "application/json";
        this.responseHeaders = headers;
    }

    public String getQueryParameter(String paramName) {
        return queryParameters.get(paramName);
    }

    public int getRequestCount() {
        return requestCount.get();
    }

    public Method getRequestMethod() {
        return requestMethod;
    }

    public String getUri() {
        return uri;
    }

    public String getBody() {
        return body;
    }

    public String getHeader(String headerName) {
        return headers.get(headerName.toLowerCase());
    }

    public int getQueryParametersCount() {
        return queryParameters.size();
    }

    public int getHeadersCount() {
        return headers.size();
    }
}