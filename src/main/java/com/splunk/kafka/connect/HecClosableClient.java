package com.splunk.kafka.connect;

import org.apache.http.impl.client.CloseableHttpClient;

import com.splunk.hecclient.HecConfig;

public abstract class HecClosableClient {
    abstract CloseableHttpClient getClient(HecConfig config);
}
