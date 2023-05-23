package org.infobip.mobile.messaging.api.support.http.client;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

class UntrustedSSLHelper {

    private static final TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
    };

    @SuppressWarnings("TrustAllX509TrustManager")
    static void trustAllCerts(HttpsURLConnection urlConnection) {
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            urlConnection.setSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            System.err.println("Cannot instantiate trust-all ssl context: " + e);
        }
    }
}
