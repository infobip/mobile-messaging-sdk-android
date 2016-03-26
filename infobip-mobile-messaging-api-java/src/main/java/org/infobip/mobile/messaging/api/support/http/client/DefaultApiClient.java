package org.infobip.mobile.messaging.api.support.http.client;

import org.infobip.mobile.messaging.api.support.ApiBackendException;
import org.infobip.mobile.messaging.api.support.ApiException;
import org.infobip.mobile.messaging.api.support.ApiIOException;
import org.infobip.mobile.messaging.api.support.Tuple;
import org.infobip.mobile.messaging.api.support.http.client.model.ApiResponse;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.api.support.util.StreamUtils;
import org.infobip.mobile.messaging.api.support.util.StringUtils;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map;

/**
 * @author mstipanov
 * @since 08.03.2016.
 */
public class DefaultApiClient implements ApiClient {
    public static final int DEFAULT_CONNECT_TIMEOUT = 5000;

    public static final int DEFAULT_READ_TIMEOUT = 60000;

    public static final JsonSerializer JSON_SERIALIZER = new JsonSerializer();

    private final int connectTimeout;
    private final int readTimeout;
    private final String osName;
    private final String osVersion;
    private final String osBuildTag;
    private final String libraryVersion;
    private final String[] userAgentAdditions;
    private String userAgent;

    public DefaultApiClient() {
        this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, null, null, null, null);
    }

    public DefaultApiClient(int connectTimeout, int readTimeout, String osName, String osVersion, String osBuildTag, String libraryVersion, String... userAgentAdditions) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.osName = osName;
        this.osVersion = osVersion;
        this.osBuildTag = osBuildTag;
        this.libraryVersion = libraryVersion;
        this.userAgentAdditions = userAgentAdditions;
    }

    @Override
    public <B, R> R execute(HttpMethod method, String uri, String apiKey, Map<String, Collection<Object>> queryParams, Map<String, Collection<Object>> headers, B body, Class<R> responseType) {
        HttpURLConnection urlConnection = null;
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Collection<Object>> entry : queryParams.entrySet()) {
                appendVaule(sb, entry);
            }

            urlConnection = (HttpURLConnection) new URL(uri + sb.toString()).openConnection();
            urlConnection.setRequestMethod(method.name());
            urlConnection.setUseCaches(false);
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setConnectTimeout(connectTimeout);
            urlConnection.setReadTimeout(readTimeout);

            if (null != headers) {
                for (Map.Entry<String, Collection<Object>> entry : headers.entrySet()) {
                    Collection<Object> value = entry.getValue();
                    if (null == value || value.isEmpty()) {
                        continue;
                    }

                    String key = entry.getKey().trim();
                    if (key.equalsIgnoreCase("Content-Length")) {
                        continue;
                    }
                    for (Object v : value) {
                        urlConnection.setRequestProperty(key, v.toString());
                    }
                }
            }
            if (StringUtils.isNotBlank(apiKey)) {
                urlConnection.setRequestProperty("Authorization", "App " + apiKey);
            }
            urlConnection.setRequestProperty("Accept", "application/json");
            String userAgent = urlConnection.getRequestProperty("User-Agent");
            if (null == userAgent) {
                urlConnection.setRequestProperty("User-Agent", getUserAgent());
            }

            if (null != body) {
                byte[] bytes = JSON_SERIALIZER.serialize(body).getBytes("UTF-8");
                urlConnection.setRequestProperty("Content-Length", "" + Long.toString(bytes.length));
                urlConnection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream outputStream = new BufferedOutputStream(urlConnection.getOutputStream())) {
                    outputStream.write(bytes);
                    outputStream.flush();
                }
            }

            int responseCode = urlConnection.getResponseCode();
            if (responseCode >= 400) {
                ApiResponse apiResponse = new ApiResponse("-1", "Unknown error");
                if (urlConnection.getContentLength() > 0) {
                    InputStream inputStream = urlConnection.getErrorStream();
                    String s = StreamUtils.readToString(inputStream, "UTF-8", Long.parseLong(urlConnection.getHeaderField("Content-Length")));
                    apiResponse = JSON_SERIALIZER.deserialize(s, ApiResponse.class);
                }
                if (responseCode >= 500) {
                    Tuple<String, String> tuple = safeGetErrorInfo(apiResponse, "-2", "Unknown API backend error");
                    throw new ApiBackendException(tuple.getLeft(), tuple.getRight());
                }

                Tuple<String, String> tuple = safeGetErrorInfo(apiResponse, "-3", "Unknown API error");
                throw new ApiException(tuple.getLeft(), tuple.getRight());
            }

            if (urlConnection.getContentLength() <= 0) {
                return null;
            }

            InputStream inputStream = urlConnection.getInputStream();
            String s = StreamUtils.readToString(inputStream, "UTF-8", Long.parseLong(urlConnection.getHeaderField("Content-Length")));
            return JSON_SERIALIZER.deserialize(s, responseType);
        } catch (Exception e) {
            if (e instanceof ApiIOException) {
                throw (ApiIOException) e;
            }
            throw new ApiIOException("-4", "Can't access URI: " + uri, e);
        } finally {
            if (null != urlConnection) {
                try {
                    urlConnection.disconnect();
                } catch (Exception e) {
                    //ignore
                }
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
        if (StringUtils.isNotBlank(osName)) {
            sb.append(osName);
        }
        sb.append(";");
        if (StringUtils.isNotBlank(osVersion)) {
            sb.append(osVersion);
        }
        sb.append(";");
        if (StringUtils.isNotBlank(osBuildTag)) {
            sb.append(osBuildTag);
        }
        sb.append(";");
        if (null != userAgentAdditions) {
            for (String userAgentAddition : userAgentAdditions) {
                sb.append(userAgentAddition);
                sb.append(";");
            }
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

        return new Tuple<>(apiResponse.getRequestError().getServiceException().getCode(), apiResponse.getRequestError().getServiceException().getText());
    }

    private void appendVaule(StringBuilder sb, Map.Entry<String, Collection<Object>> entry) throws UnsupportedEncodingException {
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
}
