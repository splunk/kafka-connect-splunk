package com.splunk.hecclient;

import org.apache.http.impl.client.CloseableHttpClient;


/**
 * Created by kchen on 10/20/17.
 */
public final class HecWithoutAck extends Hec {
    public HecWithoutAck(HecClientConfig config, PollerCallback callback) {
        this(config, HecClient.createHttpClient(config), callback);
    }

    public HecWithoutAck(HecClientConfig config, CloseableHttpClient httpClient, PollerCallback callback) {
        super(config, httpClient, new ResponsePoller(callback));
    }
}