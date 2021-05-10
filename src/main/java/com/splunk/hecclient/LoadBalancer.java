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

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public final class LoadBalancer implements LoadBalancerInf {
    private static final Logger log = LoggerFactory.getLogger(LoadBalancer.class);

    private final List<HecChannel> channels;
    private int index;
    private final ConcurrentHashMap<String, List<HecChannel>> indexerInfo; // Map of indexer URI to Channels for that indexer
    private final Set<String> discardedIndexers;
    // Used for health check
    private CloseableHttpClient httpClient;
    private final HttpContext context;
    private final HecConfig hecConfig;
    private final Header[] headers;
    private final ScheduledExecutorService scheduledExecutorService;
    private volatile boolean stopped;

    public LoadBalancer(HecConfig hecConfig, CloseableHttpClient client) {
        stopped = false;
        channels = new ArrayList<>();
        indexerInfo = new ConcurrentHashMap<>();
        discardedIndexers = new HashSet<>();
        index = 0;
        this.httpClient = client;
        this.hecConfig = hecConfig;
        this.context = HttpClientContext.create();
        // Init headers
        headers = new Header[1];
        headers[0] = new BasicHeader("Authorization", String.format("Splunk %s", hecConfig.getToken()));
        boolean keepAlive = false;
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        Runnable r = this::run;
        scheduledExecutorService.scheduleWithFixedDelay(r, 0, this.hecConfig.getlbPollInterval(), TimeUnit.MILLISECONDS);
    }

    public void setHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public synchronized void add(String indexerUrl, HecChannel channel) {
        log.debug("Adding indexer {} and channel {} to loadbalancer", indexerUrl, channel);
        if(indexerInfo.containsKey(indexerUrl)) {
            indexerInfo.get(indexerUrl).add(channel);
        } else {
            indexerInfo.put(indexerUrl, new ArrayList<>());
            indexerInfo.get(indexerUrl).add(channel);
        }
        channels.add(channel);
    }

    @Override
    public synchronized void remove(HecChannel channel) {
        log.debug("Removing channel {} from loadbalancer", channel);
        channels.removeIf(ch -> ch.equals(channel));
    }

    @Override
    public synchronized void send(final EventBatch batch) {
        if (channels.isEmpty()) {
            throw new HecException("No channels are available / registered with LoadBalancer");
        }

        for (int tried = 0; tried != channels.size(); tried++) {
            HecChannel channel = channels.get(index);
            index = (index + 1) % channels.size();
            if (!channel.hasBackPressure() && !channel.isNotAvailable()) {
                channel.send(batch);
                return;
            }
        }

        // all indexers have back pressure
        throw new HecException("All channels have back pressure");
    }

    @Override
    public int size() {
        return channels.size();
    }

    private void run() {
        log.debug("Running healthcheck at an interval of {} seconds", this.hecConfig.getlbPollInterval()/1000.0);
            Enumeration<String> iter = indexerInfo.keys();
        while (iter.hasMoreElements()) {
            String indexerToCheck = iter.nextElement();
            try {
                indexerCheckResult(sendHealthCheckRequest(indexerToCheck), indexerToCheck);
            } catch (Exception e) {
                indexerCheckResult(false, indexerToCheck);
                log.error("encountered exception when checking health of indexer {}, " +
                        "this means indexer and its channels are removed from the loadbalancer", e.getMessage());
            }
        }

    }

    private synchronized void indexerCheckResult(boolean success, String indexer) {
        log.debug("indexerInfo map has {} ", indexerInfo);
        log.debug("discardedIndexers map has {} ", discardedIndexers);
        log.debug("Channels List is : {}", channels);

        if(success) {
            if(discardedIndexers.contains(indexer)) {
                log.info("healthcheck passed for {} indexer, adding this indexer and its channels to the loadbalancer, " +
                        "this indexer was previously removed from loadbalancer", indexer);
                // Add channels for this indexer as healthcheck passed
                List<HecChannel> channelsToAdd = indexerInfo.get(indexer);
                channels.addAll(channelsToAdd);
                discardedIndexers.remove(indexer);
                index = 0;
            }
        } else {
            if(!discardedIndexers.contains(indexer)) {
                log.info("healthcheck failed for {} indexer, removing this indexer and its channels from the loadbalancer", indexer);
                // Remove channels for this indexer as healthcheck failed
                List<HecChannel> channelsToRemove = indexerInfo.get(indexer);
                channelsToRemove.forEach(this::remove);
                discardedIndexers.add(indexer);
                index = 0;
            }
        }
    }

    public boolean sendHealthCheckRequest(String indexerUrl) throws IOException {
        boolean retVal = false;
        String healthCheckEndpoint = "/services/collector/health";
        String url = indexerUrl + healthCheckEndpoint;
        final HttpGet httpGet = new HttpGet(url);
        httpGet.setHeaders(headers);
        int status = -1;
        try (CloseableHttpResponse resp = httpClient.execute(httpGet, context)) {
            status = resp.getStatusLine().getStatusCode();
        } finally {
            if (status == 200) {
                retVal = true;
            } else if (status == 400) {
                log.info("Invalid HEC token for indexer {}", url);
            } else if (status == 503) {
                log.info("HEC is unhealthy, queues are full for indexer {}", url);
            }
        }

        return retVal;
    }

    public final void close() {
        if (stopped) {
            return;
        }

        stopped = true;
        scheduledExecutorService.shutdown();
        log.info("LoadBalancer stopped");
    }
}
