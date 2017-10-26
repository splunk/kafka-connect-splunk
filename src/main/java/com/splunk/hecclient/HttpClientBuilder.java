package com.splunk.hecclient;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import java.security.cert.X509Certificate;

/**
 * Created by kchen on 10/20/17.
 */
final public class HttpClientBuilder {
    private int maxConnectionPoolSizePerDestination = 4;
    private int maxConnectionPoolSize = 4 * 2;
    private int socketTimeout = 60; // in seconds
    private int socketSendBufferSize = 8 * 1024 * 1024; // in bytes
    private boolean disableSSLCertVerification = false;

    public HttpClientBuilder setMaxConnectionPoolSizePerDestination(int connections) {
        this.maxConnectionPoolSizePerDestination = connections;
        return this;
    }

    public HttpClientBuilder setMaxConnectionPoolSize(int connections) {
        this.maxConnectionPoolSize = connections;
        return this;
    }

    public HttpClientBuilder setSocketTimeout(int timeout /*seconds*/) {
        this.socketTimeout = timeout;
        return this;
    }

    public HttpClientBuilder setSocketSendBufferSize(int bufSize /*bytes*/) {
        this.socketSendBufferSize = socketSendBufferSize;
        return this;
    }

    public HttpClientBuilder setDisableSSLCertVerification(boolean disableVerification) {
        disableSSLCertVerification = disableVerification;
        return this;
    }

    public CloseableHttpClient build() {
        SSLConnectionSocketFactory sslFactory = getSSLConnectionFactory();
        SocketConfig config = SocketConfig.custom()
                .setSndBufSize(socketSendBufferSize)
                .setSoTimeout(socketTimeout * 1000)
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();

        return HttpClients.custom()
                .useSystemProperties()
                .setSSLSocketFactory(sslFactory)
                .setMaxConnPerRoute(maxConnectionPoolSizePerDestination)
                .setMaxConnTotal(maxConnectionPoolSize)
                .setDefaultSocketConfig(config)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    private SSLConnectionSocketFactory getSSLConnectionFactory() {
        if (disableSSLCertVerification) {
            return getUnsecureSSLConnectionSocketFactory();
        } else {
            return getSecureSSLConnectionFactory();
        }
    }

    private SSLConnectionSocketFactory getUnsecureSSLConnectionSocketFactory() {
        TrustStrategy trustStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) {
                return true;
            }
        };

        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        SSLContext context = null;
        try {
            context = new SSLContextBuilder().loadTrustMaterial(trustStrategy).build();
        } catch (Exception ex) {
            throw new HecClientException("failed to create SSL connection factory", ex);
        }

        return new SSLConnectionSocketFactory(context, hostnameVerifier);
    }

    private SSLConnectionSocketFactory getSecureSSLConnectionFactory() {
        // use system default one
        return null;
    }
}
