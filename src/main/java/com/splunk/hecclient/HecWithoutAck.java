package com.splunk.hecclient;

import org.apache.http.impl.client.CloseableHttpClient;


/**
 * Created by kchen on 10/20/17.
 */
public final class HecWithoutAck extends Hec {
    public HecWithoutAck(HecConfig config, PollerCallback callback) {
        this(config, Hec.createHttpClient(config), callback);
        ownHttpClient = true;
    }

    public HecWithoutAck(HecConfig config, CloseableHttpClient httpClient, PollerCallback callback) {
        this(config, httpClient, new ResponsePoller(callback));
    }

    public HecWithoutAck(HecConfig config, CloseableHttpClient httpClient, Poller poller) {
        this(config, httpClient, poller, new LoadBalancer());
    }

    public HecWithoutAck(HecConfig config, PollerCallback callback, LoadBalancerInf loadBalancer) {
        this(config, Hec.createHttpClient(config), new ResponsePoller(callback), loadBalancer);
    }

    public HecWithoutAck(HecConfig config, CloseableHttpClient httpClient, Poller poller, LoadBalancerInf loadBalancer) {
        super(config, httpClient, poller, loadBalancer);
    }
}