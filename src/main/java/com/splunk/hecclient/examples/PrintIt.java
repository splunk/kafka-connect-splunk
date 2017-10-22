package com.splunk.hecclient.examples;

import com.splunk.hecclient.EventBatch;
import com.splunk.hecclient.PollerCallback;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kchen on 10/20/17.
 */
class PrintIt implements PollerCallback {
    private AtomicLong eventsFailed = new AtomicLong(0);
    private AtomicLong events = new AtomicLong(0);

    @Override
    public void onEventFailure(List<EventBatch> batches) {
        eventsFailed.addAndGet(batches.size());
        System.out.println("Failed: " + eventsFailed.get());
    }

    @Override
    public void onEventCommitted(List<EventBatch> batches) {
        events.addAndGet(batches.size());
        System.out.println("committed: " + events.get());
    }
}