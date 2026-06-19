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

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.Collections;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;


public class HttpClientBuilderTest {
    private static final String TEST_KEYSTORE = "./src/test/resources/keystoretest.jks";
    private static final char[] TEST_KEYSTORE_PASSWORD = "Notchangeme".toCharArray();

    @Test
    public void buildUnsecure() {
        HttpClientBuilder builder = new HttpClientBuilder();
        CloseableHttpClient client = builder.setMaxConnectionPoolSizePerDestination(1)
                .setMaxConnectionPoolSize(2)
                .setSocketSendBufferSize(1024)
                .setSocketTimeout(120)
                .setDisableSSLCertVerification(true)
                .build();
        Assert.assertNotNull(client);
    }

    @Test
    public void buildSecureDefault() {
        HttpClientBuilder builder = new HttpClientBuilder();
        CloseableHttpClient client = builder.setMaxConnectionPoolSizePerDestination(1)
                .setMaxConnectionPoolSize(2)
                .setSocketSendBufferSize(1024)
                .setSocketTimeout(120)
                .setDisableSSLCertVerification(false)
                .build();
        Assert.assertNotNull(client);
    }
    @Test
    public void buildSecureCustomKeystore() {
        HttpClientBuilder builder = new HttpClientBuilder();
        CloseableHttpClient client = builder.setMaxConnectionPoolSizePerDestination(1)
                .setMaxConnectionPoolSize(2)
                .setSocketSendBufferSize(1024)
                .setSocketTimeout(120)
                .setDisableSSLCertVerification(false)
                .setSslContext(Hec.loadCustomSSLContext("./src/test/resources/keystoretest.jks", "JKS", "Notchangeme"))
                .build();
        Assert.assertNotNull(client);
    }
    @Test
    public void buildSecureCustomKeystorePkcs12() {
        HttpClientBuilder builder = new HttpClientBuilder();
        CloseableHttpClient client = builder.setMaxConnectionPoolSizePerDestination(1)
                .setMaxConnectionPoolSize(2)
                .setSocketSendBufferSize(1024)
                .setSocketTimeout(120)
                .setDisableSSLCertVerification(false)
                .setSslContext(Hec.loadCustomSSLContext("./src/test/resources/keystoretest.p12", "PKCS12", "Notchangeme"))
                .build();
        Assert.assertNotNull(client);
    }

    @Test
    public void buildDefault() {
        HttpClientBuilder builder = new HttpClientBuilder();
        CloseableHttpClient client = builder.build();
        Assert.assertNotNull(client);
    }

    @Test
    public void buildKerberosUsesDefaultCertificateValidation() throws Exception {
        HttpsServer server = startHttpsServer();
        try (CloseableHttpClient client = createKerberosHecClient(serverUrl(server), false)) {
            try {
                client.execute(new HttpGet(serverUrl(server)));
                Assert.fail("Expected Kerberos client to reject an untrusted HTTPS certificate");
            } catch (SSLException expected) {
                Assert.assertNotNull(expected);
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void buildKerberosHonorsDisabledCertificateValidation() throws Exception {
        HttpsServer server = startHttpsServer();
        try (CloseableHttpClient client = createKerberosHecClient(serverUrl(server), true);
             CloseableHttpResponse response = client.execute(new HttpGet(serverUrl(server)))) {
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            EntityUtils.consume(response.getEntity());
        } finally {
            server.stop(0);
        }
    }

    private static HttpsServer startHttpsServer() throws Exception {
        HttpsServer server = HttpsServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(createServerSslContext()));
        server.createContext("/", exchange -> {
            byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(response);
            }
        });
        server.start();
        return server;
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

    private static SSLContext createServerSslContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (FileInputStream inputStream = new FileInputStream(TEST_KEYSTORE)) {
            keyStore.load(inputStream, TEST_KEYSTORE_PASSWORD);
        }

        KeyManagerFactory keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, TEST_KEYSTORE_PASSWORD);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);
        return sslContext;
    }

    private static String serverUrl(HttpsServer server) {
        return "https://localhost:" + server.getAddress().getPort();
    }
}
