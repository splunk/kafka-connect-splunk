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

public class Hec implements HecInf {
    private HecConfig clientConfig;
    private LoadBalancerInf loadBalancer;
    private Poller poller;
    private CloseableHttpClient httpClient;
    private boolean ownHttpClient = false;

    // factory methods
    public static Hec newHecWithAck(HecConfig config, PollerCallback callback) {
        Hec hec = newHecWithAck(config, Hec.createHttpClient(config), callback);
        hec.setOwnHttpClient(true);
        return hec;
    }

    public static Hec newHecWithAck(HecConfig config, CloseableHttpClient httpClient, PollerCallback callback) {
        return new Hec(config, httpClient, createPoller(config, callback), new LoadBalancer());
    }

    public static Hec newHecWithAck(HecConfig config, PollerCallback callback, LoadBalancerInf loadBalancer) {
        Hec hec = new Hec(config, Hec.createHttpClient(config), createPoller(config, callback), loadBalancer);
        hec.setOwnHttpClient(true);
        return hec;
    }

    public static Hec newHecWithoutAck(HecConfig config, PollerCallback callback) {
        Hec hec = newHecWithoutAck(config, Hec.createHttpClient(config), callback);
        hec.setOwnHttpClient(true);
        return hec;
    }

    public static Hec newHecWithoutAck(HecConfig config, CloseableHttpClient httpClient, PollerCallback callback) {
        return new Hec(config, httpClient, new ResponsePoller(callback), new LoadBalancer());
    }

    public static Hec newHecWithoutAck(HecConfig config, PollerCallback callback, LoadBalancerInf loadBalancer) {
        Hec hec = new Hec(config, Hec.createHttpClient(config), new ResponsePoller(callback), loadBalancer);
        hec.setOwnHttpClient(true);
        return hec;
    }

    public static HecAckPoller createPoller(HecConfig config, PollerCallback callback) {
        return new HecAckPoller(callback)
                .setAckPollInterval(config.getAckPollInterval())
                .setAckPollThreads(config.getAckPollThreads())
                .setEventBatchTimeout(config.getEventBatchTimeout());
    }

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
        this.ownHttpClient = ownHttpClient;
    }

    public Hec setOwnHttpClient(boolean ownHttpClient) {
        this.ownHttpClient = ownHttpClient;
        return this;
    }

    @Override
    public final void send (final EventBatch batch) {
        if (batch.isEmpty()) {
            return;
        }
        loadBalancer.send(batch);
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
