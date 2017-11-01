package com.splunk.hecclient;

import org.apache.http.impl.client.CloseableHttpClient;

/**
 * Created by kchen on 10/20/17.
 */
public abstract class Hec implements HecInf {
    private HecConfig clientConfig;
    private LoadBalancerInf loadBalancer;
    private Poller poller;
    private CloseableHttpClient httpClient;
    protected boolean ownHttpClient = false;

    public Hec(HecConfig config, CloseableHttpClient httpClient, Poller poller, LoadBalancerInf loadBalancer) {
        for (int i = 0; i < config.getTotalChannels();) {
            for (String uri: config.getUris()) {
                Indexer indexer = new Indexer(uri, config.getToken(), httpClient, poller);
                indexer.setKeepAlive(config.getHttpKeepAlive());
                loadBalancer.add(indexer.getChannel().setTracking(config.getEnableChannelTracking()));
                i++;
            }
        }

        this.loadBalancer = loadBalancer;
        this.poller = poller;
        this.poller.start();
        this.httpClient = httpClient;
    }

    @Override
    public final boolean send (final EventBatch batch) {
        if (batch.isEmpty()) {
            return false;
        }
        return loadBalancer.send(batch);
    }

    @Override
    public final void close() {
        poller.stop();
        if (ownHttpClient) {
            try {
                httpClient.close();
            } catch (Exception ex) {
                throw new HecException("failed to close http client", ex);
            }
        }
    }

    public static CloseableHttpClient createHttpClient(final HecConfig config) {
        int poolSizePerDest = config.getMaxHttpConnectionPerChannel();
        return new HttpClientBuilder()
                .setDisableSSLCertVerification(config.getDisableSSLCertVerification())
                .setMaxConnectionPoolSizePerDestination(poolSizePerDest)
                .setMaxConnectionPoolSize(poolSizePerDest * config.getUris().size())
                .build();
    }
}