package org.infobip.mobile.messaging.tools;

import org.infobip.mobile.messaging.api.support.Tuple;
import org.infobip.mobile.messaging.api.support.util.StreamUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import fi.iki.elonen.NanoHTTPD;

public class DebugServer extends NanoHTTPD {
    private Map<String, String> queryParameters = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private List<Tuple<String, String>> bodies = new ArrayList<>();
    private AtomicInteger requestCount = new AtomicInteger(0);
    private Response.Status status;
    private String mimeType;
    private String txt;
    private Method requestMethod;
    private String uri;

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
        bodies.add(new Tuple<>(session.getUri(), readBody(session)));

        return new Response(status, mimeType, txt);
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
        if (!bodies.isEmpty()) {
            return bodies.get(bodies.size() - 1).getRight();
        }
        return null;
    }

    public String getBody(String uri) {
        for (Tuple<String, String> t : bodies) {
            if (t.getLeft().toLowerCase().contains(uri.toLowerCase())) {
                return t.getRight();
            }
        }
        return null;
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