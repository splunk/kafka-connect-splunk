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

/**
 * Created by kchen on 10/18/17.
 */

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

    public HecAckPoller(PollerCallback cb) {
        outstandingEventBatches = new ConcurrentHashMap<>();
        totalOutstandingEventBatches = new AtomicLong(0);
        ackPollInterval = 10; // 10 seconds
        eventBatchTimeout = 2 * 60; // 2 mins
        pollThreads = 2;
        pollerCallback = cb;
        started = new AtomicBoolean(false);
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
            poll();
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

        ConcurrentHashMap<Long, EventBatch> channelEvents = outstandingEventBatches.get(channel);
        if (channelEvents == null) {
            outstandingEventBatches.putIfAbsent(channel, new ConcurrentHashMap<>());
            channelEvents = outstandingEventBatches.get(channel);
        }

        if (channelEvents.get(resp.getAckId()) != null) {
            log.error("ackId already exists for channel={} index={}", channel, channel.getIndexer());
            assert false: "ackId already exists";
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
            executorService.submit(new RunAckQuery(ackReq, channel));
        }

        if (!timeouts.isEmpty()) {
            log.warn("detected {} event batches timedout", timeouts.size());
            totalOutstandingEventBatches.addAndGet(-timeouts.size());
            pollerCallback.onEventFailure(timeouts, new HecClientException("timeouts"));
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
            String resp = channel.executeHttpRequest(request);
            handleAckPollResponse(resp, channel);
        }
    }

    private void findAndRemoveTimedoutBatches(Map<Long, EventBatch> batches, List<EventBatch> timeouts) {
        Iterator<Map.Entry<Long, EventBatch>> iterator = batches.entrySet().iterator();
        while (iterator.hasNext()) {
            EventBatch batch = iterator.next().getValue();
            if (batch.isTimedout(eventBatchTimeout)) {
                timeouts.add(batch);
                iterator.remove();
            }
        }
    }

    private void handleAckPollResponse(String resp, HecChannel channel) {
        log.debug("ackPollResponse={}", resp);
        HecAckPollResponse ackPollResult;
        try {
            ackPollResult = jsonMapper.readValue(resp, HecAckPollResponse.class);
        } catch (Exception ex) {
            log.error("failed to handle ack polled result", ex);
            return;
        }
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

    private static HttpUriRequest createAckPollHttpRequest(HecChannel ch, Set<Long> ids) {
        // Prepare the payload
        String ackIds;
        Map<String, Object> json = new HashMap<>();
        try {
            json.put("acks", ids);
            ackIds = jsonMapper.writeValueAsString(json);
        } catch (JsonProcessingException ex) {
            log.error("failed to create ack poll request", ex);
            throw new HecClientException("failed to create ack poll request", ex);
        }

        log.debug("acks={} channel={} indexer={}", ackIds, ch, ch.getIndexer());

        StringEntity entity = null;
        try {
            entity = new StringEntity(ackIds);
        } catch (UnsupportedEncodingException ex) {
            log.error("failed to create ack poll request", ex);
            throw new HecClientException("failed to create ack poll request", ex);
        }

        entity.setContentType("application/json; profile=urn:splunk:event:1.0; charset=utf-8");

        String url = ch.getIndexer().getBaseUrl() + ackEndpoint;
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeaders(ch.getIndexer().getHeaders());
        httpPost.setEntity(entity);

        return httpPost;
    }
}