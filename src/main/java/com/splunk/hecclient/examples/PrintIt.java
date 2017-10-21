package com.splunk.hecclient.examples;

import com.splunk.hecclient.EventBatch;
import com.splunk.hecclient.PollerCallback;

import java.util.List;

/**
 * Created by kchen on 10/20/17.
 */
class PrintIt implements PollerCallback {
    private int eventsFailed = 0;
    private int events = 0;

    @Override
    public void onEventFailure(List<EventBatch> batches) {
        for (EventBatch batch: batches) {
            eventsFailed += 1;
        }
        System.out.println("Failed: " + eventsFailed);
    }

    @Override
    public void onEventCommitted(List<EventBatch> batches) {
        for (EventBatch batch: batches) {
            events += 1;
        }
        System.out.println("committed: " + events);
    }
}