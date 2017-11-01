package com.splunk.hecclient;

import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Created by kchen on 10/20/17.
 */
public final class HecWithAck extends Hec {
    public HecWithAck(HecConfig config, PollerCallback callback) {
        this(config, Hec.createHttpClient(config), callback);
        ownHttpClient = true;
    }

    public HecWithAck(HecConfig config, CloseableHttpClient httpClient, PollerCallback callback) {
        this(config, httpClient, createPoller(config, callback));
    }

    public HecWithAck(HecConfig config, CloseableHttpClient httpClient, Poller poller) {
        this(config, httpClient, poller, new LoadBalancer());
    }

    public HecWithAck(HecConfig config, PollerCallback callback, LoadBalancerInf loadBalancer) {
        this(config, Hec.createHttpClient(config), createPoller(config, callback), loadBalancer);
        ownHttpClient = true;
    }

    public HecWithAck(HecConfig config, CloseableHttpClient httpClient, Poller poller, LoadBalancerInf loaderBalancer) {
        super(config, httpClient, poller, loaderBalancer);
    }

    public static HecAckPoller createPoller(HecConfig config, PollerCallback callback) {
        return new HecAckPoller(callback)
                .setAckPollInterval(config.getAckPollInterval())
                .setAckPollThreads(config.getAckPollThreads())
                .setEventBatchTimeout(config.getEventBatchTimeout());
    }
}