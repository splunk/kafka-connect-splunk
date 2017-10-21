package com.splunk.hecclient;

import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Created by kchen on 10/17/17.
 */

// HecClient is not threadsafe
public class HecClient {
    private HecClientConfig config;
    private IndexerCluster cluster;

    public HecClient(HecClientConfig config, CloseableHttpClient client, Poller poller) {
        this.config = config;

        cluster = new IndexerCluster();
        for (String uri: config.getUris()) {
            Indexer indexer = new Indexer(uri, config.getToken(), client, poller);
            indexer.setKeepAlive(config.getHttpKeepAlive());
            cluster.add(indexer);
        }
    }

    public static CloseableHttpClient createHttpClient(HecClientConfig config) {
        int poolSizePerDest = config.getMaxHttpConnectionPoolSizePerIndexer();
        return new HttpClientBuilder()
                .setDisableSSLCertVerification(config.getDisableSSLCertVerification())
                .setMaxConnectionPoolSizePerDestination(poolSizePerDest)
                .setMaxConnectionPoolSize(poolSizePerDest * config.getUris().size())
                .build();
    }

    public void send(EventBatch batch) {
        cluster.send(batch);
    }
}