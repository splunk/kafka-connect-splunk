package com.splunk.hecclient;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kchen on 10/31/17.
 */
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
    public void buildSecure() {
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
    public void buildDefault() {
        HttpClientBuilder builder = new HttpClientBuilder();
        CloseableHttpClient client = builder.build();
        Assert.assertNotNull(client);
    }
}
