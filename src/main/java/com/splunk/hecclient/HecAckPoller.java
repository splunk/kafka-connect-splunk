package com.splunk.hecclient;

import com.splunk.hecclient.errors.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.FutureRequestExecutionService;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kchen on 10/18/17.
 */
public class HecAckPoller implements AckPoller {
    private final static Logger log = LoggerFactory.getLogger(HecAckPoller.class);
    private final static ObjectMapper jsonMapper = new ObjectMapper();

    private final static String ackEndpoint = "/services/collector/ack";

    private CloseableHttpClient httpClient;
    private ConcurrentHashMap<HecChannel, ConcurrentHashMap<Long, EventBatch>> outstandingEventBatches;
    private ConcurrentHashMap<HecChannel, AtomicLong> eventStartingIds;
    private AtomicLong totalOutstandingEventBatches;
    private int expirationDuration; // in seconds
    private int ackPollInterval; // in seconds
    private int pollThreads;
    private AckCallback ackCallback;
    FutureRequestExecutionService ackPollerService;
    private ScheduledExecutorService scheduler;
    private AtomicBoolean started;

    // HecAckPoller owns client
    public HecAckPoller(CloseableHttpClient client, AckCallback cb) {
        httpClient = client;
        outstandingEventBatches = new ConcurrentHashMap<>();
        eventStartingIds = new ConcurrentHashMap<>();
        totalOutstandingEventBatches = new AtomicLong(0);
        ackPollInterval = 10; // 10 seconds
        expirationDuration = 2 * 60; // 2 mins
        pollThreads = 4;
        ackCallback = cb;
        started = new AtomicBoolean(false);
    }

    @Override
    public void start() {
        if (started.compareAndSet(false, true)) {
            ExecutorService executorService = Executors.newFixedThreadPool(pollThreads);
            ackPollerService = new FutureRequestExecutionService(httpClient, executorService);
            ThreadFactory f = (Runnable r) -> new Thread(r, "ACK poller");
            scheduler = Executors.newScheduledThreadPool(1, f);
            Runnable poller = () -> {
                poll();
            };
            scheduler.scheduleWithFixedDelay(poller, ackPollInterval, ackPollInterval, TimeUnit.SECONDS);
        }
    }

    @Override
    public void stop() {
        if (started.compareAndSet(true, false)) {
            scheduler.shutdownNow();
            try {
                ackPollerService.close();
            } catch (IOException ex) {
                log.error("failed to close ack poller service", ex);
                throw new ShutdownException("failed to close ack poller service", ex);
            }
        }
    }

    @Override
    public HecChannel minLoadChannel() {
        return minChannel(eventStartingIds);
    }

    @Override
    public HecChannel currentMinLoadChannel() {
        return minChannel(outstandingEventBatches);
    }

    private static <T, R> T minChannel(ConcurrentHashMap<T, R> map) {
        T minCh = null;
        long minEvents = Long.MAX_VALUE;

        // Find channel which has min outstanding events
        for (Map.Entry<T, R> entry: map.entrySet()) {
            T ch = entry.getKey();
            R val = entry.getValue();

            long size = 0;
            if (val instanceof ConcurrentHashMap<?, ?>) {
                size = ((ConcurrentHashMap<?, ?>) val).size();
            } else if (val instanceof AtomicLong){
                size = ((AtomicLong) val).longValue();
            } else {
                assert false: "invalid type " + val;
            }

            if (size < minEvents) {
                minCh = ch;
                minEvents = size;
            }
        }
        return minCh;
    }

    @Override
    public long getTotalOutstandingEventBatches() {
        return totalOutstandingEventBatches.longValue();
    }

    @Override
    public void add(HecChannel channel, EventBatch batch) {
        AtomicLong eventId = eventStartingIds.get(channel);
        if (eventId == null) {
            eventStartingIds.putIfAbsent(channel, new AtomicLong(0));
            eventId = eventStartingIds.get(channel);
        }
        Long eid = eventId.getAndIncrement();

        ConcurrentHashMap<Long, EventBatch> channelEvents = outstandingEventBatches.get(channel);
        if (channelEvents == null) {
            outstandingEventBatches.putIfAbsent(channel, new ConcurrentHashMap<>());
            channelEvents = outstandingEventBatches.get(channel);
        }

        channelEvents.put(eid, batch);

        // increase total number of event batches
        totalOutstandingEventBatches.incrementAndGet();
    }

    // setPollThreads before calling start
    public HecAckPoller setPollThreads(int num) {
        pollThreads = num;
        return this;
    }

    // setExpirationDuration before calling start
    public HecAckPoller setExpirationDuration(int duration) {
        expirationDuration = duration;
        return this;
    }

    // setAckPollInterval before calling start
    public HecAckPoller setAckPollInterval(int interval) {
        ackPollInterval = interval;
        return this;
    }

    private void poll() {
        for (Map.Entry<HecChannel, ConcurrentHashMap<Long, EventBatch>> entry: outstandingEventBatches.entrySet()) {
            Set<Long> ids = entry.getValue().keySet();
            if (ids.isEmpty()) {
                continue;
            }
            HttpUriRequest ackReq = createAckPollRequest(entry.getKey(), ids);
            // FIXME null context
            ackPollerService.execute(ackReq, HttpClientContext.create(), new AckResponseHandler(), new AckPollResponseHandler(entry.getKey()));
        }
    }

    private final class AckResponseHandler implements ResponseHandler<HttpResponse> {
        @Override
        public HttpResponse handleResponse(final HttpResponse response) {
            // just pass through
            return response;
        }
    }

    private final class AckPollResponse {
        private final SortedMap<String, Boolean> acks = new TreeMap<>();

        Collection<Long> getSuccessIds() {
            Set<Long> successful = new HashSet<>();
            for(Map.Entry<String,Boolean> e: acks.entrySet()){
                if(e.getValue()) { // was 'true' in json, meaning it succeeded
                    successful.add(Long.parseLong(e.getKey()));
                }
            }
            return successful;
        }

        public Map<String, Boolean> getAcks() {
            return acks;
        }
    }

    private final class AckPollResponseHandler implements FutureCallback<HttpResponse> {
        private HecChannel channel;

        public AckPollResponseHandler(HecChannel ch) {
            channel = ch;
        }

        @Override
        public void completed(HttpResponse resp) {
            int code = resp.getStatusLine().getStatusCode();

            String payload;
            try {
                payload = EntityUtils.toString(resp.getEntity());
            } catch (Exception ex) {
                log.error("failed to handle ack poll response", ex);
                return;
            }

            if (code != 200 || code != 201) {
                log.error("failed to poll ack", payload);
                return;
            }

            try {
                AckPollResponse ackPollResult = jsonMapper.readValue(payload, AckPollResponse.class);
                handleAckPollResult(channel, ackPollResult);
            } catch (Exception ex) {
                log.error("failed to handle ack polled result", ex);
                return;
            }
        }

        @Override
        public void failed(final Exception ex) {
            log.error("failed to poll acks", ex);
        }

        @Override
        public void cancelled() {
            log.error("cancel to poll acks");
        }
    }

    private void handleAckPollResult(HecChannel channel, AckPollResponse result) {
        Collection<Long> ids = result.getSuccessIds();
        if (ids.isEmpty()) {
            return;
        }

        List<EventBatch> batches = new ArrayList<>();
        ConcurrentHashMap<Long, EventBatch> channelBatches = outstandingEventBatches.get(channel);
        for (Long id: ids) {
            EventBatch batch = channelBatches.remove(id);
            if (batch == null) {
                log.warn(String.format("event batch id={} for channel={} on host={} is not in map anymore",
                        id, channel.getId(), channel.getIndexer().getBaseUrl()));
                assert false: "inconsistent state for event batch ack states";
                continue;
            }
            batches.add(batch);
        }

        if (!batches.isEmpty() && ackCallback != null) {
            ackCallback.onEventAcked(batches);
        }
    }

    private HttpUriRequest createAckPollRequest(HecChannel ch, Set<Long> ids) {
        // Prepare the payload
        String ackIds;
        Map json = new HashMap();
        try {
            json.put("acks", ids);
            ackIds = jsonMapper.writeValueAsString(json);
        } catch (JsonProcessingException ex) {
            log.error("failed to create ack poll request", ex);
            throw new InvalidDataException("failed to create ack poll request", ex);
        }

        StringEntity entity = null;
        try {
            entity = new StringEntity(ackIds);
        } catch (UnsupportedEncodingException ex) {
            log.error("failed to create ack poll request", ex);
            throw new InvalidDataException("failed to create ack poll request", ex);
        }

        entity.setContentType("application/json; profile=urn:splunk:event:1.0; charset=utf-8");

        String url = ch.getIndexer().getBaseUrl() + ackEndpoint;
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeaders(ch.getIndexer().getHeaders());
        httpPost.setEntity(entity);

        return httpPost;
    }
}