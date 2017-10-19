package com.splunk.hecclient;

import org.apache.http.impl.client.CloseableHttpClient;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kchen on 10/18/17.
 */
public class HecAckPoller implements AckPoller {
    private CloseableHttpClient httpClient;
    private ConcurrentHashMap<HecChannel, ConcurrentHashMap<Long, EventBatch>> outstandingEventBatches;
    private ConcurrentHashMap<HecChannel, AtomicLong> eventStartingIds;

    public HecAckPoller(CloseableHttpClient client) {
        httpClient = client;
        outstandingEventBatches = new ConcurrentHashMap<>();
        eventStartingIds = new ConcurrentHashMap<>();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void add(HecChannel channel, EventBatch batch) {
        AtomicLong eventId = eventStartingIds.get(channel);
        if (eventId == null) {
            eventStartingIds.putIfAbsent(channel, new AtomicLong(1));
            eventId = eventStartingIds.get(channel);
        }
        Long eid = eventId.getAndIncrement();

        ConcurrentHashMap<Long, EventBatch> channelEvents = outstandingEventBatches.get(channel);
        if (channelEvents == null) {
            outstandingEventBatches.putIfAbsent(channel, new ConcurrentHashMap<>());
            channelEvents = outstandingEventBatches.get(channel);
        }

        channelEvents.put(eid, batch);
    }
}
