package org.infobip.mobile.messaging.api.support.http.client;

import org.infobip.mobile.messaging.api.support.ApiBackendException;
import org.infobip.mobile.messaging.api.support.ApiBackendExceptionWithContent;
import org.infobip.mobile.messaging.api.support.ApiException;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.Tuple;
import org.infobip.mobile.messaging.api.support.http.client.model.ApiResponse;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.api.support.util.Base64Encoder;
import org.infobip.mobile.messaging.api.support.util.StreamUtils;
import org.infobip.mobile.messaging.api.support.util.StringUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
public class DefaultApiClient implements ApiClient {
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    public static final int DEFAULT_READ_TIMEOUT = 60000;

    public static final JsonSerializer JSON_SERIALIZER = new JsonSerializer();
    private static final JsonSerializer JSON_SERIALIZER_WITH_NULLS = new JsonSerializer(true);

    private final int connectTimeout;
    private final int readTimeout;
    private final String libraryVersion;
    private final String[] userAgentAdditions;
    private final RequestInterceptor requestInterceptors[];
    private final ResponsePreProcessor responsePreProcessors[];
    private final Logger logger;
    private final boolean allowUntrustedSSLOnError;
    private String userAgent;

    public DefaultApiClient() {
        this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, null, new RequestInterceptor[0], new ResponsePreProcessor[0], new Logger(), false);
    }

    public DefaultApiClient(int connectTimeout, int readTimeout, String libraryVersion, RequestInterceptor interceptors[], ResponsePreProcessor responsePreProcessors[], Logger logger, boolean allowUntrustedSSLOnError, String... userAgentAdditions) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.libraryVersion = libraryVersion;
        this.requestInterceptors = interceptors;
        this.responsePreProcessors = responsePreProcessors;
        this.userAgentAdditions = userAgentAdditions;
        this.logger = logger;
        this.allowUntrustedSSLOnError = allowUntrustedSSLOnError;
    }

    @Override
    public <B, R> R execute(HttpMethod method, String uri, String apiKey, Tuple<String, String> credentials, Map<String, Collection<Object>> queryParams, Map<String, Collection<Object>> headers, B body, Class<R> responseType) {
        Request request = new Request(method, uri, apiKey, credentials, headers, queryParams, body);
        for (RequestInterceptor interceptor : requestInterceptors) {
            try {
                request = interceptor.intercept(request);
            } catch (Exception e) {
                logger.e("Request interceptor " + interceptor + " thrown an exception " + e);
            }
        }

        logger.d("REQUEST: " + request);

        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Collection<Object>> entry : request.queryParams.entrySet()) {
                appendValue(sb, entry);
            }

            try {
                return executeHTTP(request, responseType, false);
            } catch (SSLHandshakeException ex) {
                logger.w("Got SSL handshake exception " + ex);
                if (allowUntrustedSSLOnError) {
                    logger.w("Will re-try in untrusted mode");
                    return executeHTTP(request, responseType, true);
                } else {
                    throw ex;
                }
            }
        } catch (Exception e) {
            interceptErrorResponse(e);
            if (e instanceof ApiIOException) {
                throw (ApiIOException) e;
            }
            throw new ApiIOException(ErrorCode.API_IO_ERROR.value, ErrorCode.API_IO_ERROR.description + " : " + request.uri, e);
        }
    }

    private <R> R executeHTTP(Request request, Class<R> responseType, boolean tryUntrustedSSL) throws IOException {
        HttpURLConnection urlConnection = null;
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Collection<Object>> entry : request.queryParams.entrySet()) {
                appendValue(sb, entry);
            }

            urlConnection = (HttpURLConnection) new URL(request.uri + sb.toString()).openConnection();

            if (request.httpMethod == HttpMethod.PATCH) {
                urlConnection.setRequestProperty("X-HTTP-Method-Override", HttpMethod.PATCH.name());
                urlConnection.setRequestMethod(HttpMethod.POST.name());
            } else {
                urlConnection.setRequestMethod(request.httpMethod.name());
            }

            if (request.httpMethod != HttpMethod.GET) {
                urlConnection.setDoOutput(true);
            }

            urlConnection.setUseCaches(false);
            urlConnection.setDoInput(true);
            urlConnection.setConnectTimeout(connectTimeout);
            urlConnection.setReadTimeout(readTimeout);

            if (tryUntrustedSSL && urlConnection instanceof HttpsURLConnection) {
                UntrustedSSLHelper.trustAllCerts(((HttpsURLConnection) urlConnection));
            }

            if (null != request.headers) {
                for (Map.Entry<String, Collection<Object>> entry : request.headers.entrySet()) {
                    Collection<Object> value = entry.getValue();
                    if (null == value || value.isEmpty()) {
                        continue;
                    }

                    String key = entry.getKey().trim();
                    if (key.equalsIgnoreCase("Content-Length")) {
                        continue;
                    }
                    for (Object v : value) {
                        if (v == null) continue;
                        urlConnection.setRequestProperty(key, v.toString());
                    }
                }
            }
            if (StringUtils.isNotBlank(request.apiKey)) {
                urlConnection.setRequestProperty("Authorization", "App " + request.apiKey);
            } else if (request.credentials != null && StringUtils.isNotBlank(request.credentials.getLeft()) && StringUtils.isNotBlank(request.credentials.getRight())) {
                String basicApiKey = Base64Encoder.encode(request.credentials.getLeft() + ":" + request.credentials.getRight());
                urlConnection.setRequestProperty("Authorization", "Basic " + basicApiKey);
            }
            urlConnection.setRequestProperty("Accept", "application/json");
            String userAgent = urlConnection.getRequestProperty("User-Agent");
            if (null == userAgent) {
                urlConnection.setRequestProperty("User-Agent", getUserAgent());
            }

            if (null != request.body) {
                byte[] bytes = jsonSerializer(request.httpMethod).serialize(request.body).getBytes("UTF-8");
                urlConnection.setRequestProperty("Content-Length", "" + Long.toString(bytes.length));
                urlConnection.setRequestProperty("Content-Type", "application/json");
                OutputStream outputStream = null;
                try {
                    outputStream = new BufferedOutputStream(urlConnection.getOutputStream());
                    outputStream.write(bytes);
                    outputStream.flush();
                } finally {
                    StreamUtils.closeSafely(outputStream);
                }
            }

            int responseCode = urlConnection.getResponseCode();
            interceptResponse(responseCode, urlConnection.getHeaderFields());
            if (responseCode >= 400) {
                ApiResponse apiResponse = new ApiResponse(ErrorCode.UNKNOWN_ERROR.value, ErrorCode.UNKNOWN_ERROR.description);
                if (urlConnection.getContentLength() > 0) {
                    InputStream inputStream = urlConnection.getErrorStream();
                    String s = StreamUtils.readToString(inputStream, "UTF-8", Long.parseLong(urlConnection.getHeaderField("Content-Length")));
                    apiResponse = jsonSerializer(request.httpMethod).deserialize(s, ApiResponse.class);
                }

                if (responseCode >= 500) {
                    Tuple<String, String> tuple = safeGetErrorInfo(apiResponse, ErrorCode.UNKNOWN_API_BACKEND_ERROR.value, ErrorCode.UNKNOWN_API_BACKEND_ERROR.description);
                    throw new ApiBackendException(tuple.getLeft(), tuple.getRight());
                }

                Tuple<String, String> tuple = safeGetErrorInfo(apiResponse, ErrorCode.UNKNOWN_API_ERROR.value, ErrorCode.UNKNOWN_API_ERROR.description);
                throw new ApiException(tuple.getLeft(), tuple.getRight());
            }

            if (urlConnection.getContentLength() <= 0) {
                return null;
            }

            if (Void.class.equals(responseType) || void.class.equals(responseType)) {
                return null;
            }

            InputStream inputStream = urlConnection.getInputStream();
            String s = StreamUtils.readToString(inputStream, "UTF-8", Long.parseLong(urlConnection.getHeaderField("Content-Length")));
            inputStream.close();

            R response = jsonSerializer(request.httpMethod).deserialize(s, responseType);
            ApiResponse apiResponse = null;
            try {
                apiResponse = jsonSerializer(request.httpMethod).deserialize(s, ApiResponse.class);
            } catch (Exception ignored) {
            }

            if (apiResponse != null && apiResponse.getRequestError() != null) {
                Tuple<String, String> tuple = safeGetErrorInfo(apiResponse, ErrorCode.UNKNOWN_API_BACKEND_ERROR.value, ErrorCode.UNKNOWN_API_BACKEND_ERROR.description);
                throw new ApiBackendExceptionWithContent(tuple.getLeft(), tuple.getRight(), response);
            }

            return response;
        } finally {
            try {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            } catch (Exception ignored) {

            }
        }
    }

    private void interceptErrorResponse(Exception error) {
        for (ResponsePreProcessor responsePreProcessor : responsePreProcessors) {
            try {
                responsePreProcessor.beforeResponse(error);
            } catch (Exception e) {
                logger.e("Response interceptor " + responsePreProcessor + " thrown an exception " + e);
            }
        }
    }

    private void interceptResponse(int httpStatusCode, Map<String, List<String>> responseHeaders) {
        for (ResponsePreProcessor responsePreProcessor : responsePreProcessors) {
            try {
                responsePreProcessor.beforeResponse(httpStatusCode, responseHeaders);
            } catch (Exception e) {
                logger.e("Response interceptor " + responsePreProcessor + " thrown an exception " + e);
            }
        }
    }

    private String getUserAgent() {
        if (null != userAgent) {
            return userAgent;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Infobip Mobile API Client");
        if (StringUtils.isNotBlank(libraryVersion)) {
            sb.append("/").append(libraryVersion);
        }
        sb.append("(");
        if (null != userAgentAdditions) {
            sb.append(StringUtils.join(";", userAgentAdditions));
        }
        sb.append(")");
        userAgent = sb.toString();
        return userAgent;
    }

    private Tuple<String, String> safeGetErrorInfo(ApiResponse apiResponse, String code, String message) {
        if (null == apiResponse) {
            return new Tuple<>(code, message);
        }

        if (null == apiResponse.getRequestError()) {
            return new Tuple<>(code, message);
        }

        if (null == apiResponse.getRequestError().getServiceException()) {
            return new Tuple<>(code, message);
        }

        return new Tuple<>(apiResponse.getRequestError().getServiceException().getMessageId(), apiResponse.getRequestError().getServiceException().getText());
    }

    private void appendValue(StringBuilder sb, Map.Entry<String, Collection<Object>> entry) throws UnsupportedEncodingException {
        if (null == entry) {
            return;
        }

        Collection<Object> value = entry.getValue();
        if (null == value || value.isEmpty()) {
            return;
        }

        for (Object v : value) {
            String s = v.toString();
            if (StringUtils.isBlank(s)) {
                s = "";
            }
            sb.append(sb.length() == 0 ? "?" : "&").append(URLEncoder.encode(entry.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(s, "UTF-8"));
        }
    }

    private static JsonSerializer jsonSerializer(HttpMethod httpMethod) {
        switch (httpMethod) {
            case PATCH: return JSON_SERIALIZER_WITH_NULLS;
            default: return JSON_SERIALIZER;
        }
    }

    public enum ErrorCode {
        UNKNOWN_ERROR("-1", "Unknown error"), // responseCode >= 400
        UNKNOWN_API_BACKEND_ERROR("-2", "Unknown API backend error"), // responseCode >= 500
        UNKNOWN_API_ERROR("-3", "Unknown API error"), // apiResponse == null
        API_IO_ERROR("-4", "Can't access URI"); // Can't access URI (404?)

        private final String value;
        private final String description;

        ErrorCode(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }
}
