package com.splunk.hecclient;

import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Created by kchen on 10/17/17.
 */

// HecClient is not multi-thread safe
public class HecClient {
    private HecClientConfig config;
    private LoadBalancer loadBalancer;

    public HecClient(HecClientConfig config, CloseableHttpClient client, Poller poller) {
        this.config = config;

        loadBalancer = new LoadBalancer();
        for (int i = 0; i < config.getTotalChannelNumber();) {
            for (String uri: config.getUris()) {
                Indexer indexer = new Indexer(uri, config.getToken(), client, poller);
                indexer.setKeepAlive(config.getHttpKeepAlive());
                loadBalancer.add(indexer.getChannel());
                i++;
            }
        }
    }

    public static CloseableHttpClient createHttpClient(HecClientConfig config) {
        int poolSizePerDest = config.getMaxHttpConnectionPerChannel();
        return new HttpClientBuilder()
                .setDisableSSLCertVerification(config.getDisableSSLCertVerification())
                .setMaxConnectionPoolSizePerDestination(poolSizePerDest)
                .setMaxConnectionPoolSize(poolSizePerDest * config.getUris().size())
                .build();
    }

    public void send(EventBatch batch) {
        loadBalancer.send(batch);
    }

    public void sendAckPollRequests() {
        loadBalancer.sendAckPollRequests();
    }
}