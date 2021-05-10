/*
 * Copyright 2017 Splunk, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

public final class HttpClientBuilder {
    private int maxConnectionPoolSizePerDestination = 4;
    private int maxConnectionPoolSize = 4 * 2;
    private int socketTimeout = 60; // in seconds
    private int socketSendBufferSize = 8 * 1024 * 1024; // in bytes
    private boolean disableSSLCertVerification = false;
    private SSLContext sslContext = null;

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
        this.socketSendBufferSize = bufSize;
        return this;
    }

    public HttpClientBuilder setDisableSSLCertVerification(boolean disableVerification) {
        disableSSLCertVerification = disableVerification;
        return this;
    }

    public HttpClientBuilder setSslContext(SSLContext context) {
        this.sslContext = context;
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
        TrustStrategy trustStrategy = (chain, authType) -> true;

        HostnameVerifier hostnameVerifier = (hostname, session) -> true;

        try {
            this.sslContext = new SSLContextBuilder().loadTrustMaterial(trustStrategy).build();
        } catch (Exception ex) {
            throw new HecException("failed to create SSL connection factory", ex);
        }

        return new SSLConnectionSocketFactory(this.sslContext, hostnameVerifier);
    }

    private SSLConnectionSocketFactory getSecureSSLConnectionFactory() {
        if (this.sslContext == null) {
            return null; // use system default one
        } else {
            return new SSLConnectionSocketFactory(this.sslContext);
        }
    }
}
