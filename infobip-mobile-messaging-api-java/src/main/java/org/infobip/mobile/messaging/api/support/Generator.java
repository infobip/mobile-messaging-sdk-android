package org.infobip.mobile.messaging.api.support;

import static org.infobip.mobile.messaging.api.support.util.ReflectionUtils.loadPackageInfo;

import org.infobip.mobile.messaging.api.support.http.ApiKey;
import org.infobip.mobile.messaging.api.support.http.Body;
import org.infobip.mobile.messaging.api.support.http.Credentials;
import org.infobip.mobile.messaging.api.support.http.FullUrl;
import org.infobip.mobile.messaging.api.support.http.Header;
import org.infobip.mobile.messaging.api.support.http.Headers;
import org.infobip.mobile.messaging.api.support.http.HttpRequest;
import org.infobip.mobile.messaging.api.support.http.Path;
import org.infobip.mobile.messaging.api.support.http.Paths;
import org.infobip.mobile.messaging.api.support.http.Queries;
import org.infobip.mobile.messaging.api.support.http.Query;
import org.infobip.mobile.messaging.api.support.http.Version;
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;
import org.infobip.mobile.messaging.api.support.http.client.Logger;
import org.infobip.mobile.messaging.api.support.http.client.RequestInterceptor;
import org.infobip.mobile.messaging.api.support.http.client.ResponsePreProcessor;
import org.infobip.mobile.messaging.api.support.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Data;
import lombok.NonNull;

/**
 * Generates Mobile API proxies.
 * <pre>{@code
 * MobileApiRegistration mobileApiRegistration = new Generator.Builder().build().create(MobileApiRegistration.class);
 * }</pre>
 *
 * @author mstipanov
 * @see Builder
 * @since 17.03.2016.
 */
@Data
public class Generator {

    private DefaultApiClient apiClient;
    private String baseUrl = "https://mobile.infobip.com/";
    private ConcurrentHashMap<Class<?>, CachingInvocationHandler> proxyCacheMap = new ConcurrentHashMap<>();
    private Properties properties = System.getProperties();
    private int connectTimeout = DefaultApiClient.DEFAULT_CONNECT_TIMEOUT;
    private int readTimeout = DefaultApiClient.DEFAULT_READ_TIMEOUT;
    private String[] userAgentAdditions = new String[0];
    private RequestInterceptor[] requestInterceptors = new RequestInterceptor[0];
    private ResponsePreProcessor[] responsePreProcessors = new ResponsePreProcessor[0];
    private Logger logger = new Logger();
    private boolean allowUntrustedSSLOnError = false;

    private DefaultApiClient getApiClient() {
        if (null != apiClient) {
            return apiClient;
        }
        String libraryVersion = properties.getProperty("library.version");
        apiClient = new DefaultApiClient(connectTimeout, readTimeout, libraryVersion, requestInterceptors, responsePreProcessors, logger, allowUntrustedSSLOnError, userAgentAdditions);
        return apiClient;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(@NonNull Class<T> type) {
        CachingInvocationHandler<?> cachingInvocationHandler = proxyCacheMap.get(type);
        if (null == cachingInvocationHandler) {
            cachingInvocationHandler = new CachingInvocationHandler<>(type);
            proxyCacheMap.put(type, cachingInvocationHandler);
        }
        return (T) cachingInvocationHandler.getProxy();
    }

    private Version getVersionAnnotation(Method method) {
        Version version = method.getAnnotation(Version.class);
        if (null != version) {
            return version;
        }

        version = method.getDeclaringClass().getAnnotation(Version.class);
        if (null != version) {
            return version;
        }

        return getPackageAnnotation(method.getDeclaringClass().getPackage().getName(), Version.class);
    }

    private ApiKey getApiKeyAnnotation(Method method) {
        ApiKey apiKey = method.getAnnotation(ApiKey.class);
        if (null != apiKey) {
            return apiKey;
        }

        apiKey = method.getDeclaringClass().getAnnotation(ApiKey.class);
        if (null != apiKey) {
            return apiKey;
        }

        return getPackageAnnotation(method.getDeclaringClass().getPackage().getName(), ApiKey.class);
    }

    private Credentials getCredentialsAnnotation(Method method) {
        Credentials credentials = method.getAnnotation(Credentials.class);
        if (null != credentials) {
            return credentials;
        }

        credentials = method.getDeclaringClass().getAnnotation(Credentials.class);
        if (null != credentials) {
            return credentials;
        }

        return getPackageAnnotation(method.getDeclaringClass().getPackage().getName(), Credentials.class);
    }

    private <T extends Annotation> T getPackageAnnotation(String pkg, Class<T> annotationClass) {
        Package p = Package.getPackage(pkg);
        if (null == p) {
            try {
                loadPackageInfo(pkg);
                p = Package.getPackage(pkg);
            } catch (Exception e) {
                //ignore
            }
        }
        if (null != p) {
            T t = p.getAnnotation(annotationClass);
            if (null != t) {
                return t;
            }
        }

        if (pkg.length() == 0) {
            return null;
        }

        int endIndex = pkg.lastIndexOf('.');
        if (endIndex < 0) {
            pkg = "";
        } else {
            pkg = pkg.substring(0, endIndex);
        }
        return getPackageAnnotation(pkg, annotationClass);
    }

    @SuppressWarnings("unchecked")
    private <T> T getAnnotation(Annotation[] annotations, Class<T> type) {
        for (Annotation annotation : annotations) {
            if (!type.isAssignableFrom(annotation.annotationType())) {
                continue;
            }
            return (T) annotation;
        }
        return null;
    }

    private Collection<Object> toCollection(Object arg) {
        if (null == arg) {
            return null;
        }

        if (arg instanceof Collection) {
            return (Collection) arg;
        }

        if (arg.getClass().isArray()) {
            int length = Array.getLength(arg);
            ArrayList<Object> objects = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                objects.add(Array.get(arg, i));
            }
            return objects;
        }

        return Collections.singleton(arg);
    }

    /**
     * Builds {@link Generator}
     *
     * @see Generator
     * @see Builder#withBaseUrl(String)
     * @see Builder#withProperties(Properties)
     * @see Builder#withConnectTimeout(int)
     * @see Builder#withReadTimeout(int)
     * @see Builder#withUserAgentAdditions(String...)
     */
    public static class Builder {
        private final Generator generator;

        public Builder() {
            generator = new Generator();
        }

        /**
         * It will set the base API URL. By default it will be <a href="https://mobile.infobip.com/">https://mobile.infobip.com/</a>
         *
         * @return {@link Builder}
         */
        public Builder withBaseUrl(@NonNull String baseUrl) {
            generator.setBaseUrl(baseUrl);
            return this;
        }

        /**
         * It will set the base properties from which User-Agent header will be constructed.
         * <p>
         * Used properties:
         * <ul>
         * <li><i>api.key</i> - for Mobile API API-KEY. You must use application code as the API-KEY!</li>
         * <li><i>library.version</i> - for Mobile Messaging library version</li>
         * </ul>
         *
         * @return {@link Builder}
         */
        public Builder withProperties(@NonNull Properties properties) {
            generator.setProperties(properties);
            return this;
        }

        /**
         * It will set the connect timeout. Default is: 5s
         *
         * @return {@link Builder}
         */
        public Builder withConnectTimeout(int connectTimeout) {
            generator.setConnectTimeout(connectTimeout);
            return this;
        }

        /**
         * It will set the read timeout. Default is: 1min
         *
         * @return {@link Builder}
         */
        public Builder withReadTimeout(int readTimeout) {
            generator.setReadTimeout(readTimeout);
            return this;
        }

        /**
         * It will custom string array to append to User-Agent header.
         *
         * @return {@link Builder}
         */
        public Builder withUserAgentAdditions(@NonNull String... userAgentAdditions) {
            generator.setUserAgentAdditions(userAgentAdditions);
            return this;
        }

        /**
         * It will set request interceptors for http client.
         *
         * @param requestInterceptors interceptors to add to chain.
         * @return {@link Builder}
         */
        public Builder withRequestInterceptors(@NonNull RequestInterceptor... requestInterceptors) {
            generator.requestInterceptors = requestInterceptors;
            return this;
        }

        /**
         * Will set response header interceptors for http client
         *
         * @param responsePreProcessors interceptors to add
         * @return {@link Builder}
         */
        public Builder withResponseHeaderInterceptors(@NonNull ResponsePreProcessor... responsePreProcessors) {
            generator.responsePreProcessors = responsePreProcessors;
            return this;
        }


        /**
         * Will set custom logger for http client
         * @param logger logger
         * @return {@link Builder}
         */
        public Builder withLogger(@NonNull Logger logger) {
            generator.logger = logger;
            return this;
        }

        /**
         * Will allow untrusted SSL if `true` supplied
         * @param allowUntrustedSSLOnError setting
         * @return {@link Builder}
         */
        public Builder withAllowUntrustedSSLOnError(boolean allowUntrustedSSLOnError) {
            generator.allowUntrustedSSLOnError = allowUntrustedSSLOnError;
            return this;
        }

        public Generator build() {
            if (StringUtils.isBlank(generator.getBaseUrl())) {
                throw new IllegalArgumentException("baseUrl is mandatory");
            }

            return generator;
        }
    }

    @Data
    private class CachingInvocationHandler<T> implements InvocationHandler {
        private final Class<T> type;
        private ConcurrentHashMap<Method, ProxyCache> proxyCacheMap = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        public T getProxy() {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            ProxyCache proxyCache = getProxyCache(method);
            String uri;

            Map<String, Collection<Object>> queryParams = new HashMap<>(proxyCache.getDefaultQueryParams());
            Map<String, Collection<Object>> headerMap = new HashMap<>(proxyCache.getDefaultHeaderMap());
            String apiKey = proxyCache.getApiKey();
            Tuple<String, String> credentials = proxyCache.getCredentials();

            Parameter[] parameters = proxyCache.getParameters();
            Object body = null;

            boolean hasFullUrl = false;
            boolean hasUriAnnotations = false;

            uri = StringUtils.join("/", baseUrl, proxyCache.getUri());

            for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
                Parameter parameter = parameters[i];
                Object arg = args[i];

                if (null != parameter.getBody()) {
                    body = arg;
                }

                FullUrl fullUrl = parameter.getFullUrl();
                if (null != fullUrl) {
                    uri = arg.toString();
                    hasFullUrl = true;
                }

                Version version = parameter.getVersion();
                if (null != version) {
                    uri = uri.replace("{version}", arg.toString());
                    hasUriAnnotations = true;
                }

                Path path = parameter.getPath();
                if (null != path) {
                    uri = uri.replace("{" + path.name() + "}", arg.toString());
                    hasUriAnnotations = true;
                }

                Query q = parameter.getQuery();
                if (null != q) {
                    String name = q.name();
                    Collection<Object> value = toCollection(arg);
                    if (value != null) {
                        queryParams.put(name, value);
                    }
                    hasUriAnnotations = true;
                }

                Header h = parameter.getHeader();
                if (null != h) {
                    String name = h.name();
                    headerMap.put(name, toCollection(arg));
                }
            }

            // Validate after processing that @FullUrl wasn't mixed with URI-building annotations
            if (hasFullUrl && hasUriAnnotations) {
                throw new IllegalArgumentException("@FullUrl cannot be combined with @Version, @Path, or @Query annotations");
            }

            if (uri != null && uri.endsWith("/")) {
                uri = uri.substring(0, uri.length() - 1);
            }

            return getApiClient().execute(getHttpRequestMethod(proxyCache.httpRequests), uri, apiKey, credentials, queryParams, headerMap, body, method.getReturnType());
        }

        private HttpMethod getHttpRequestMethod(HttpRequest[] httpRequests) {
            HttpMethod method = HttpMethod.GET;
            for (HttpRequest httpRequest : httpRequests) {
                method = httpRequest.method();
            }
            return method;
        }

        private ProxyCache getProxyCache(Method method) {
            ProxyCache proxyCache = proxyCacheMap.get(method);
            if (null != proxyCache) {
                return proxyCache;
            }

            proxyCache = new ProxyCache(method);
            proxyCacheMap.put(method, proxyCache);
            return proxyCache;
        }
    }

    @Data
    private class Parameter {
        private final Body body;
        private final Path path;
        private final Query query;
        private final Header header;
        private final Version version;
        private final FullUrl fullUrl;
    }

    @Data
    private class ProxyCache {
        private final HttpRequest[] httpRequests;
        private final String uri;
        private final HashMap<String, Collection<Object>> defaultQueryParams;
        private final HashMap<String, Collection<Object>> defaultHeaderMap;
        private final Parameter[] parameters;
        private final String apiKey;
        private final Tuple<String, String> credentials;

        public ProxyCache(Method method) {
            this.httpRequests = createHttpRequest(method);
            this.uri = createUri(method);
            this.defaultQueryParams = createDefaultQueryParams(method);
            this.defaultHeaderMap = createDefaultHeaderMap(method);
            this.parameters = createParameters(method);
            this.apiKey = findApiKey(method);
            this.credentials = findCredentials(method);
        }

        private String findApiKey(Method method) {
            ApiKey apiKeyAnotation = getApiKeyAnnotation(method);
            if (null == apiKeyAnotation) {
                return null;
            }
            return injectProperty(apiKeyAnotation.value());
        }

        private Tuple<String, String> findCredentials(Method method) {
            Credentials credentialsAnnotation = getCredentialsAnnotation(method);
            if (null == credentialsAnnotation) {
                return null;
            }
            return new Tuple<>(injectProperty(credentialsAnnotation.user()), injectProperty(credentialsAnnotation.password()));
        }

        private Parameter[] createParameters(Method method) {
            Annotation[][] parameterAnnotations = method.getParameterAnnotations();
            Parameter[] parameters = new Parameter[parameterAnnotations.length];
            int i = 0;
            for (Annotation[] annotations : parameterAnnotations) {
                Parameter parameter = new Parameter(
                        getAnnotation(annotations, Body.class)
                        , getAnnotation(annotations, Path.class)
                        , getAnnotation(annotations, Query.class)
                        , getAnnotation(annotations, Header.class)
                        , getAnnotation(annotations, Version.class)
                        , getAnnotation(annotations, FullUrl.class)
                );
                parameters[i++] = parameter;
            }
            return parameters;
        }

        private HttpRequest[] createHttpRequest(Method method) {
            HttpRequest httpRequestOnClass = method.getDeclaringClass().getAnnotation(HttpRequest.class);
            HttpRequest httpRequest = method.getAnnotation(HttpRequest.class);
            if (null != httpRequest && null != httpRequestOnClass) {
                return new HttpRequest[]{httpRequestOnClass, httpRequest};
            }
            if (null != httpRequest) {
                return new HttpRequest[]{httpRequest};
            }
            if (null != httpRequestOnClass) {
                return new HttpRequest[]{httpRequestOnClass};
            }
            throw new NoHttpRequestAnnotation("Method '" + method.getName() + "' must be annotated with @HttpRequest!");
        }

        private String createUri(Method method) {
            String uri = getHttpRequestValue();
            Version version = getVersionAnnotation(method);
            if (null != version) {
                uri = uri.replace("{version}", injectProperty(version.value()));
            }

            Path path = method.getAnnotation(Path.class);
            if (null != path) {
                uri = uri.replace("{" + path.name() + "}", injectProperty(path.value()));
            }
            Paths paths = method.getAnnotation(Paths.class);
            if (null != paths) {
                for (Path p : paths.value()) {
                    uri = uri.replace("{" + p.name() + "}", injectProperty(p.value()));
                }
            }
            return uri;
        }

        private String getHttpRequestValue() {
            String uri = "";
            for (HttpRequest httpRequest : httpRequests) {
                uri = StringUtils.join("/", uri, httpRequest.value());
            }
            return uri;
        }

        private String injectProperty(String s) {
            if (StringUtils.isBlank(s)) {
                return s;
            }
            if (s.startsWith("${") && s.endsWith("}")) {
                s = s.substring(2, s.length() - 1);
                String[] split = s.split(":");
                return properties.getProperty(split[0], split.length > 1 ? split[1] : null);
            }
            return s;
        }

        private Collection<Object> injectProperty(String[] strings) {
            if (null == strings) {
                return null;
            }

            ArrayList<Object> objects = new ArrayList<>();
            for (String s : strings) {
                objects.add(injectProperty(s));
            }
            return objects;
        }

        private HashMap<String, Collection<Object>> createDefaultQueryParams(Method method) {
            HashMap<String, Collection<Object>> queryParams = new HashMap<>();
            Query query = method.getAnnotation(Query.class);
            if (null != query) {
                queryParams.put(query.name(), injectProperty(query.value()));
            }
            Queries queries = method.getAnnotation(Queries.class);
            if (null != queries) {
                for (Query q : queries.value()) {
                    queryParams.put(q.name(), injectProperty(q.value()));
                }
            }
            return queryParams;
        }

        private HashMap<String, Collection<Object>> createDefaultHeaderMap(Method method) {
            HashMap<String, Collection<Object>> headerMap = new HashMap<>();
            Header header = method.getAnnotation(Header.class);
            if (null != header) {
                headerMap.put(header.name(), injectProperty(header.value()));
            }
            Headers headers = method.getAnnotation(Headers.class);
            if (null != headers) {
                for (Header q : headers.value()) {
                    headerMap.put(q.name(), injectProperty(q.value()));
                }
            }

            return headerMap;
        }
    }
}
