package com.splunk.hecclient;

import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Created by kchen on 10/20/17.
 */
// Hec class owns poller, httpClient etc resource
public abstract class Hec {
    private HecClient client;
    private Poller poller;
    private CloseableHttpClient httpClient;
    protected boolean ownHttpClient = false;

    public Hec(HecClientConfig config, CloseableHttpClient httpClient, Poller poller) {
        client = new HecClient(config, httpClient, poller);
        this.poller = poller;
        this.poller.start();
        this.httpClient = httpClient;
    }

    public void send(EventBatch batch) {
        if (batch.isEmpty()) {
            return;
        }
        client.send(batch);
    }

    public void close() {
        poller.stop();
        if (ownHttpClient) {
            try {
                httpClient.close();
            } catch (Exception ex) {
                throw new HecClientException("failed to close http client", ex);
            }
        }
    }
}