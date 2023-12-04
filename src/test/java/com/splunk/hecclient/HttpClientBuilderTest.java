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

import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;


public class HttpClientBuilderTest {
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
    public void buildWithDisabledHostnameVerification() {
        HttpClientBuilder builder = new HttpClientBuilder();
        CloseableHttpClient client = builder.setMaxConnectionPoolSizePerDestination(1)
            .setMaxConnectionPoolSize(2)
            .setSocketSendBufferSize(1024)
            .setSocketTimeout(120)
            .setDisableSSLCertVerification(false)
            .setDisableHostnameVerification(true)
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
                .setSslContext(Hec.loadCustomSSLContext("./src/test/resources/keystoretest.jks","Notchangeme"))
                .build();
        Assert.assertNotNull(client);
    }

    @Test
    public void buildDefault() {
        HttpClientBuilder builder = new HttpClientBuilder();
        CloseableHttpClient client = builder.build();
        Assert.assertNotNull(client);
    }
}