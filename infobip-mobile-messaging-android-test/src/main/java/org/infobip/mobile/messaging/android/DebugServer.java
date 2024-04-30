//package org.infobip.mobile.messaging.android;
//
//
//import android.util.Pair;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import fi.iki.elonen.NanoHTTPD;
//
//public class DebugServer extends NanoHTTPD {
//    private Map<String, String> queryParameters = new HashMap<>();
//    private Map<String, String> headers = new HashMap<>();
//    private Map<String, String> responseHeaders = new HashMap<>();
//    private List<Pair<String, String>> bodies = new ArrayList<>();
//    private AtomicInteger requestCount = new AtomicInteger(0);
//    private Response.Status status;
//    private String mimeType;
//    private String txt;
//    private Method requestMethod;
//    private String uri;
//    private InputStream inputStream;
//
//    public DebugServer() {
//        super(0);
//    }
//
//    @Override
//    public Response serve(IHTTPSession session) {
//        requestCount.incrementAndGet();
//
//        requestMethod = session.getMethod();
//        uri = session.getUri();
//        queryParameters = session.getParms();
//        headers = session.getHeaders();
//        bodies.add(new Pair<>(session.getUri(), readBody(session)));
//
//        Response response;
//        if (inputStream != null) {
//            response = new Response(status, mimeType, inputStream);
//        } else {
//            response = new Response(status, mimeType, txt);
//        }
//
//        for (String key : responseHeaders.keySet()) {
//            response.addHeader(key, responseHeaders.get(key));
//        }
//
//        return response;
//    }
//
//    private String readBody(IHTTPSession session) {
//        if (null != headers) {
//            String contentLength = headers.get("content-length");
//            if (null != contentLength) {
//                long length = Long.parseLong(contentLength);
//                if (length > 0) {
//                    try {
//                        return readToString(session.getInputStream(), "UTF-8", length);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//        return null;
//    }
//
//    public void respondWith(Response.Status status, String mimeType, InputStream inputStream) {
//        this.txt = null;
//        this.bodies.clear();
//        this.status = status;
//        this.mimeType = mimeType;
//        this.inputStream = inputStream;
//    }
//
//    public void respondWith(Response.Status status, String json) {
//        this.respondWith(status, "application/json", json);
//    }
//
//    public void respondWith(Response.Status status, String mimeType, String txt) {
//        this.inputStream = null;
//        this.bodies.clear();
//        this.status = status;
//        this.mimeType = mimeType;
//        this.txt = txt;
//    }
//
//    public void respondWith(Response.Status status, String body, Map<String, String> headers) {
//        this.status = status;
//        this.txt = body;
//        this.mimeType = "application/json";
//        this.responseHeaders = headers;
//    }
//
//    public String getQueryParameter(String paramName) {
//        return queryParameters.get(paramName);
//    }
//
//    public int getRequestCount() {
//        return requestCount.get();
//    }
//
//    public Method getRequestMethod() {
//        return requestMethod;
//    }
//
//    public String getUri() {
//        return uri;
//    }
//
//    public String getBody() {
//        if (!bodies.isEmpty()) {
//            return bodies.get(bodies.size() - 1).second;
//        }
//        return null;
//    }
//
//    public String getBody(String uri) {
//        for (Pair<String, String> t : bodies) {
//            if (t.first.toLowerCase().contains(uri.toLowerCase())) {
//                return t.second;
//            }
//        }
//        return null;
//    }
//
//    public List<String> getBodiesForUri(String uri) {
//        List<String> filteredBodies = new ArrayList<>();
//        for (Pair<String, String> body : bodies) {
//            if (body.first.toLowerCase().contains(uri.toLowerCase())) {
//                filteredBodies.add(body.second);
//            }
//        }
//        return filteredBodies;
//    }
//
//    public String getHeader(String headerName) {
//        return headers.get(headerName.toLowerCase());
//    }
//
//    public int getQueryParametersCount() {
//        return queryParameters.size();
//    }
//
//    public int getHeadersCount() {
//        return headers.size();
//    }
//
//    private String readToString(InputStream inputStream, String charsetName, long length) throws IOException {
//        ByteArrayOutputStream buf = new ByteArrayOutputStream();
//        int result = inputStream.read();
//        long count = 1;
//        while (result != -1) {
//            byte b = (byte) result;
//            buf.write(b);
//            if (count == length) {
//                break;
//            }
//            result = inputStream.read();
//            count++;
//        }
//
//        String stringFromStream = buf.toString(charsetName);
//        if (stringFromStream.length() < 1) {
//            return "";
//        }
//        return stringFromStream;
//    }
//}