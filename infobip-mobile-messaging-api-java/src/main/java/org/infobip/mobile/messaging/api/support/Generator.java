package org.infobip.mobile.messaging.api.support;

import lombok.Data;
import lombok.NonNull;
import org.infobip.mobile.messaging.api.support.http.*;
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient;
import org.infobip.mobile.messaging.api.support.http.client.HttpMethod;
import org.infobip.mobile.messaging.api.support.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.infobip.mobile.messaging.api.support.util.ReflectionUtils.loadPackageInfo;

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
    private String baseUrl = "https://oneapi.infobip.com/";
    private ConcurrentHashMap<Class<?>, CachingInvocationHandler> proxyCacheMap = new ConcurrentHashMap<>();
    private Properties properties = System.getProperties();
    private int connectTimeout = DefaultApiClient.DEFAULT_CONNECT_TIMEOUT;
    private int readTimeout = DefaultApiClient.DEFAULT_READ_TIMEOUT;
    private String[] userAgentAdditions = new String[0];

    private DefaultApiClient getApiClient() {
        if (null != apiClient) {
            return apiClient;
        }
        String libraryVersion = properties.getProperty("library.version");
        apiClient = new DefaultApiClient(connectTimeout, readTimeout, libraryVersion, userAgentAdditions);
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

    private User getUserAnnotation(Method method) {
        User user = method.getAnnotation(User.class);
        if (null != user) {
            return user;
        }

        user = method.getDeclaringClass().getAnnotation(User.class);
        if (null != user) {
            return user;
        }

        return getPackageAnnotation(method.getDeclaringClass().getPackage().getName(), User.class);
    }

    private Password getPasswordAnnotation(Method method) {
        Password password = method.getAnnotation(Password.class);
        if (null != password) {
            return password;
        }

        password = method.getDeclaringClass().getAnnotation(Password.class);
        if (null != password) {
            return password;
        }

        return getPackageAnnotation(method.getDeclaringClass().getPackage().getName(), Password.class);
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
         * It will set the base API URL. By default it will be <a href="https://oneapi.infobip.com/">https://oneapi.infobip.com/</a>
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
//            HttpRequest httpRequest = proxyCache.getHttpRequest();
            String uri = proxyCache.getUri();

            Map<String, Collection<Object>> queryParams = new HashMap<>(proxyCache.getDefaultQueryParams());
            Map<String, Collection<Object>> headerMap = new HashMap<>(proxyCache.getDefaultHeaderMap());
            String apiKey = proxyCache.getApiKey();
            String user = proxyCache.getUser();
            String password = proxyCache.getPassword();

            Parameter[] parameters = proxyCache.getParameters();
            Object body = null;
            for (int i = 0, parametersLength = parameters.length; i < parametersLength; i++) {
                Parameter parameter = parameters[i];
                Object arg = args[i];
                if (null != parameter.getBody()) {
                    body = arg;
                }

                Version version = parameter.getVersion();
                if (null != version) {
                    uri = uri.replace("{version}", arg.toString());
                }

                Path p = parameter.getPath();
                if (null != p) {
                    uri = uri.replace("{" + p.name() + "}", arg.toString());
                }

                Query q = parameter.getQuery();
                if (null != q) {
                    String name = q.name();
                    queryParams.put(name, toCollection(arg));
                }

                Header h = parameter.getHeader();
                if (null != h) {
                    String name = h.name();
                    headerMap.put(name, toCollection(arg));
                }
            }

            return getApiClient().execute(getHttpRequestMethod(proxyCache.httpRequests), uri, apiKey, user, password, queryParams, headerMap, body, method.getReturnType());
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
    }

    @Data
    private class ProxyCache {
        private final HttpRequest[] httpRequests;
        private final String uri;
        private final HashMap<String, Collection<Object>> defaultQueryParams;
        private final HashMap<String, Collection<Object>> defaultHeaderMap;
        private final Parameter[] parameters;
        private final String apiKey;
        private final String user;
        private final String password;

        public ProxyCache(Method method) {
            this.httpRequests = createHttpRequest(method);
            this.uri = createUri(method);
            this.defaultQueryParams = createDefaultQueryParams(method);
            this.defaultHeaderMap = createDefaultHeaderMap(method);
            this.parameters = createParameters(method);
            this.apiKey = findApiKey(method);
            this.user = findUser(method);
            this.password = findPassword(method);
        }

        private String findApiKey(Method method) {
            ApiKey apiKeyAnotation = getApiKeyAnnotation(method);
            if (null == apiKeyAnotation) {
                return null;
            }
            return injectProperty(apiKeyAnotation.value());
        }

        private String findUser(Method method) {
            User userAnnotation = getUserAnnotation(method);
            if (null == userAnnotation) {
                return null;
            }
            return injectProperty(userAnnotation.value());
        }

        private String findPassword(Method method) {
            Password passwordAnnotation = getPasswordAnnotation(method);
            if (null == passwordAnnotation) {
                return null;
            }
            return injectProperty(passwordAnnotation.value());
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
            String uri = StringUtils.join("/", baseUrl, getHttpRequestValue());
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
