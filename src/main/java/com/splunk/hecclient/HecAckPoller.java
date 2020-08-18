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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

// HecAckPoller, it is multi-thread safe class
public final class HecAckPoller implements Poller {
    private static final Logger log = LoggerFactory.getLogger(HecAckPoller.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private static final String ackEndpoint = "/services/collector/ack";

    private ConcurrentHashMap<HecChannel, ConcurrentHashMap<Long, EventBatch>> outstandingEventBatches;
    private AtomicLong totalOutstandingEventBatches;
    private int eventBatchTimeout; // in seconds
    private int ackPollInterval; // in seconds
    private int pollThreads;
    private PollerCallback pollerCallback;
    private ScheduledThreadPoolExecutor scheduler;
    private ExecutorService executorService;
    private AtomicBoolean started;
    private AtomicBoolean stickySessionStarted;

    public HecAckPoller(PollerCallback cb) {
        outstandingEventBatches = new ConcurrentHashMap<>();
        totalOutstandingEventBatches = new AtomicLong(0);
        ackPollInterval = 10; // 10 seconds
        eventBatchTimeout = 2 * 60; // 2 mins
        pollThreads = 2;
        pollerCallback = cb;
        started = new AtomicBoolean(false);
        stickySessionStarted = new AtomicBoolean(false);
    }

    public void setStickySessionToTrue() {
        stickySessionStarted.compareAndSet(false, true);
    }

    @Override
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        ThreadFactory f = (Runnable r) -> new Thread(r, "HEC-ACK-poller-scheduler");
        scheduler = new ScheduledThreadPoolExecutor(1, f);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        scheduler.setRemoveOnCancelPolicy(true);

        Runnable poller = () -> {
            try {
                poll();
            } catch (HecException e) {
                log.error("failed to poll", e);
            }
        };
        scheduler.scheduleWithFixedDelay(poller, ackPollInterval, ackPollInterval, TimeUnit.SECONDS);

        ThreadFactory e = (Runnable r) -> new Thread(r, "HEC-ACK-poller");
        executorService = Executors.newFixedThreadPool(pollThreads, e);
    }

    @Override
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }

        scheduler.shutdownNow();
        executorService.shutdownNow();
        log.info("HecAckPoller stopped with {} outstanding un-ACKed events", totalOutstandingEventBatches.get());
    }

    @Override
    public HecChannel getMinLoadChannel() {
        HecChannel minCh = null;
        long minEvents = Long.MAX_VALUE;

        // Find channel which has min outstanding events
        for (Map.Entry<HecChannel, ConcurrentHashMap<Long, EventBatch>> entry: outstandingEventBatches.entrySet()) {
            HecChannel ch = entry.getKey();
            Map<Long, EventBatch> val = entry.getValue();

            if (val.size() < minEvents) {
                minCh = ch;
                minEvents = val.size();
            }
        }
        return minCh;
    }

    @Override
    public long getTotalOutstandingEventBatches() {
        return totalOutstandingEventBatches.get();
    }

    @Override
    public void add(HecChannel channel, EventBatch batch, String response) {
        PostResponse resp = null;
        try {
            resp = jsonMapper.readValue(response, PostResponse.class);
        } catch (Exception ex) {
            log.error("failed to parse response", ex);
            fail(channel, batch, ex);
            return;
        }
        
        if (resp.getText() == "Invalid data format") {
            log.warn("Invalid Splunk HEC data format. Ignoring events. channel={} index={} events={}", channel, channel.getIndexer(), batch.toString());
            batch.commit();
            List<EventBatch> committedBatches = new ArrayList<>();
            committedBatches.add(batch);
            pollerCallback.onEventCommitted(committedBatches);
            return;
        }

        ConcurrentHashMap<Long, EventBatch> channelEvents = outstandingEventBatches.get(channel);
        if (channelEvents == null) {
            outstandingEventBatches.putIfAbsent(channel, new ConcurrentHashMap<>());
            channelEvents = outstandingEventBatches.get(channel);
        }
        
        if (channelEvents.get(resp.getAckId()) != null) {
            log.warn("ackId={} already exists for channel={} index={} data may be duplicated in Splunk", resp.getAckId(), channel, channel.getIndexer());
            return;
        }

        channelEvents.put(resp.getAckId(), batch);

        // increase total number of event batches
        totalOutstandingEventBatches.incrementAndGet();
    }

    @Override
    public void fail(HecChannel channel, EventBatch batch, Exception ex) {
        batch.fail();
        if (pollerCallback != null) {
            pollerCallback.onEventFailure(Arrays.asList(batch), ex);
        }
    }

    public boolean isStarted() {
        return started.get();
    }

    // setAckPollThreads before calling start
    public HecAckPoller setAckPollThreads(int num) {
        pollThreads = num;
        return this;
    }

    // setEventBatchTimeout before calling start
    public HecAckPoller setEventBatchTimeout(int timeout) {
        eventBatchTimeout = timeout;
        return this;
    }

    // setAckPollInterval before calling start
    public HecAckPoller setAckPollInterval(int interval) {
        ackPollInterval = interval;
        return this;
    }

    public int getAckPollThreads() {
        return pollThreads;
    }

    public int getEventBatchTimeout() {
        return eventBatchTimeout;
    }

    public int getAckPollInterval() {
        return ackPollInterval;
    }

    /**
     * StickySessionHandler is used to reassign channel id and fail the batches for that HecChannel.
     * Also, the HecChannel will be unavailable during this period.
     * StickySessionHandler follows the following flow:
     * 1) Set channel unavailable
     * 2) Get batches for the channel
     * 3) Remove batches for the channel from the poller
     * 4) Remove batches from kafka record tracker to fail them and resend
     * 5) Remove channel
     * 6) Change channel id
     * 7) Set channel available
     *
     * @param  channel  HecChannel is the channel for which id has tobe changed and batches have to be failed.
     * @see          HecChannel
     * @since        1.1.0
     */
    public void stickySessionHandler(HecChannel channel) {
        if (!stickySessionStarted.get()) {
            return;
        }
        String oldChannelId = channel.getId();
        channel.setAvailable(false);
        log.info("Channel {} set to be not available", oldChannelId);
        ConcurrentHashMap<Long, EventBatch> channelBatches = outstandingEventBatches.get(channel);
        if(channelBatches != null && channelBatches.size() > 0) {
            log.info("Failing {} batches for the channel {}, these will be resent by the connector.", channelBatches.size(), oldChannelId);
            if (pollerCallback != null) {
                List<EventBatch> expired = new ArrayList<>();
                Iterator<Map.Entry<Long,EventBatch>> iter = channelBatches.entrySet().iterator();
                while(iter.hasNext()) {
                    Map.Entry<Long, EventBatch> pair = iter.next();
                    EventBatch batch = pair.getValue();
                    totalOutstandingEventBatches.decrementAndGet();
                    batch.fail();
                    expired.add(batch);
                    iter.remove();
                }
                pollerCallback.onEventFailure(expired, new HecException("sticky_session_expired"));
            }
        }
        outstandingEventBatches.remove(channel);
        channel.setId();
        String newChannelId = channel.getId();
        log.info("Changed channel id from {} to {}", oldChannelId, newChannelId);

        channel.setAvailable(true);
        log.info("Channel {} is available", newChannelId);
        stickySessionStarted.compareAndSet(true, false);
    }

    private void poll() {
        if (totalOutstandingEventBatches.get() <= 0 || outstandingEventBatches.size() <= 0) {
            return;
        }

        log.info("start polling {} outstanding acks for {} channels", totalOutstandingEventBatches.get(), outstandingEventBatches.size());

        List<EventBatch> timeouts = new ArrayList<>();
        for (Map.Entry<HecChannel, ConcurrentHashMap<Long, EventBatch>> entry: outstandingEventBatches.entrySet()) {
            Map<Long, EventBatch> batches = entry.getValue();
            findAndRemoveTimedoutBatches(batches, timeouts);

            Set<Long> ids = batches.keySet();
            if (ids.isEmpty()) {
                continue;
            }
            HecChannel channel = entry.getKey();
            log.info("polling {} acks for channel={} on indexer={}", ids.size(), channel, channel.getIndexer());
            HttpUriRequest ackReq = createAckPollHttpRequest(entry.getKey(), ids);
            if (ackReq != null) {
                executorService.submit(new RunAckQuery(ackReq, channel));
            }
        }

        if (!timeouts.isEmpty()) {
            log.warn("detected {} event batches timedout", timeouts.size());
            totalOutstandingEventBatches.addAndGet(-timeouts.size());
            if (pollerCallback != null) {
                pollerCallback.onEventFailure(timeouts, new HecException("timeouts"));
            }
        }
    }

    private final class RunAckQuery implements Runnable {
        private HecChannel channel;
        private HttpUriRequest request;

        RunAckQuery(HttpUriRequest req, HecChannel ch) {
            channel = ch;
            request = req;
        }

        @Override
        public void run() {
            try {
                String resp = channel.executeHttpRequest(request);
                handleAckPollResponse(resp, channel);
            } catch (Exception ex) {
                String msg = String.format("failed to poll ack for channel=%s on indexer=%s", channel, channel.getIndexer());
                log.error(msg, ex);
            }
        }
    }

    private void findAndRemoveTimedoutBatches(Map<Long, EventBatch> batches, List<EventBatch> timeouts) {
        Iterator<Map.Entry<Long, EventBatch>> iterator = batches.entrySet().iterator();
        while (iterator.hasNext()) {
            EventBatch batch = iterator.next().getValue();
            if (batch.isTimedout(eventBatchTimeout)) {
                batch.fail();
                timeouts.add(batch);
                iterator.remove();
            }
        }
    }

    private void handleAckPollResponse(String resp, HecChannel channel) {
        log.debug("ackPollResponse={}, channel={}", resp, channel);
        HecAckPollResponse ackPollResult;
        try {
            ackPollResult = jsonMapper.readValue(resp, HecAckPollResponse.class);
        } catch (Exception ex) {
            log.error("failed to handle ack polled result", ex);
            return;
        }
        stickySessionHandler(channel);
        handleAckPollResult(channel, ackPollResult);
    }

    private void handleAckPollResult(HecChannel channel, HecAckPollResponse result) {
        Collection<Long> ids = result.getSuccessIds();
        if (ids.isEmpty()) {
            log.info("no ackIds are ready for channel={} on indexer={}", channel, channel.getIndexer());
            return;
        }

        log.info("polled {} acks for channel={} on indexer={}", ids.size(), channel, channel.getIndexer());

        List<EventBatch> committedBatches = new ArrayList<>();
        ConcurrentHashMap<Long, EventBatch> channelBatches = outstandingEventBatches.get(channel);
        // Added null check as channelBatches might still be null(It may be removed while handling sticky sessions and not added until we send more data)
        if (channelBatches != null) {
            for (Long id: ids) {
                EventBatch batch = channelBatches.remove(id);
                if (batch == null) {
                    log.warn("event batch id={} for channel={} on host={} is not in map anymore", id, channel, channel.getIndexer());
                    continue;
                }
                totalOutstandingEventBatches.decrementAndGet();
                batch.commit();
                committedBatches.add(batch);
            }

            if (!committedBatches.isEmpty() && pollerCallback != null) {
                pollerCallback.onEventCommitted(committedBatches);
            }
        }
    }

    private static HttpUriRequest createAckPollHttpRequest(HecChannel ch, Set<Long> ids) {
        // Prepare the payload
        String ackIds;
        Map<String, Object> json = new HashMap<>();
        try {
            json.put("acks", ids);
            ackIds = jsonMapper.writeValueAsString(json);
        } catch (JsonProcessingException ex) {
            log.error("failed to create ack poll request", ex);
            return null;
        }

        log.debug("acks={} channel={} indexer={}", ackIds, ch, ch.getIndexer());

        StringEntity entity = null;
        try {
            entity = new StringEntity(ackIds);
        } catch (UnsupportedEncodingException ex) {
            log.error("failed to create ack poll request", ex);
            return null;
        }

        entity.setContentType("application/json; profile=urn:splunk:event:1.0; charset=utf-8");

        String url = ch.getIndexer().getBaseUrl() + ackEndpoint;
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeaders(ch.getIndexer().getHeaders());
        httpPost.setEntity(entity);

        return httpPost;
    }
}
