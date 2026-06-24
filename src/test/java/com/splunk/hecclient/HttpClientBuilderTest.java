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

import java.util.Collections;
import javax.net.ssl.SSLException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


class HttpClientBuilderTest {
    @Test
    void buildUnsecure() {
        // given
        HttpClientBuilder builder = new HttpClientBuilder();

        // when
        CloseableHttpClient client = builder.setMaxConnectionPoolSizePerDestination(1)
                .setMaxConnectionPoolSize(2)
                .setSocketSendBufferSize(1024)
                .setSocketTimeout(120)
                .setDisableSSLCertVerification(true)
                .build();

        // then
        Assert.assertNotNull(client);
    }

    @Test
    void buildSecureDefault() {
        // given
        HttpClientBuilder builder = new HttpClientBuilder();

        // when
        CloseableHttpClient client = builder.setMaxConnectionPoolSizePerDestination(1)
                .setMaxConnectionPoolSize(2)
                .setSocketSendBufferSize(1024)
                .setSocketTimeout(120)
                .setDisableSSLCertVerification(false)
                .build();

        // then
        Assert.assertNotNull(client);
    }
    @Test
    void buildSecureCustomKeystore() {
        // given
        HttpClientBuilder builder = new HttpClientBuilder();

        // when
        CloseableHttpClient client = builder.setMaxConnectionPoolSizePerDestination(1)
                .setMaxConnectionPoolSize(2)
                .setSocketSendBufferSize(1024)
                .setSocketTimeout(120)
                .setDisableSSLCertVerification(false)
                .setSslContext(Hec.loadCustomSSLContext("./src/test/resources/keystoretest.jks", "JKS", "Notchangeme"))
                .build();

        // then
        Assert.assertNotNull(client);
    }
    @Test
    void buildSecureCustomKeystorePkcs12() {
        // given
        HttpClientBuilder builder = new HttpClientBuilder();

        // when
        CloseableHttpClient client = builder.setMaxConnectionPoolSizePerDestination(1)
                .setMaxConnectionPoolSize(2)
                .setSocketSendBufferSize(1024)
                .setSocketTimeout(120)
                .setDisableSSLCertVerification(false)
                .setSslContext(Hec.loadCustomSSLContext("./src/test/resources/keystoretest.p12", "PKCS12", "Notchangeme"))
                .build();

        // then
        Assert.assertNotNull(client);
    }

    @Test
    void buildDefault() {
        // given
        HttpClientBuilder builder = new HttpClientBuilder();

        // when
        CloseableHttpClient client = builder.build();

        // then
        Assert.assertNotNull(client);
    }

    @Test
    @ExtendWith(HttpsServerExtension.class)
    void buildKerberosUsesDefaultCertificateValidation(
            HttpsServerExtension.Server httpsServer
    ) throws Exception {
        // given
        try (CloseableHttpClient client = createKerberosHecClient(httpsServer.url(), false)) {
            try {
                // when
                client.execute(new HttpGet(httpsServer.url()));

                // then
                Assert.fail("Expected Kerberos client to reject an untrusted HTTPS certificate");
            } catch (SSLException expected) {
                // then
                Assert.assertNotNull(expected);
            }
        }
    }

    @Test
    @ExtendWith(HttpsServerExtension.class)
    void buildKerberosHonorsDisabledCertificateValidation(
            HttpsServerExtension.Server httpsServer
    ) throws Exception {
        // given
        // when
        try (CloseableHttpClient client = createKerberosHecClient(httpsServer.url(), true);
             CloseableHttpResponse response = client.execute(new HttpGet(httpsServer.url()))) {
            // then
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
        }
    }

    private static CloseableHttpClient createKerberosHecClient(
            String uri,
            boolean disableSslCertVerification
    ) {
        HecConfig config = new HecConfig(Collections.singletonList(uri), "token")
            .setKerberosPrincipal("user@EXAMPLE.COM")
            .setDisableSSLCertVerification(disableSslCertVerification);
        return Hec.createHttpClient(config);
    }
}
