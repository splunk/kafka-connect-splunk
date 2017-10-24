package com.splunk.kafka.connect;

import com.splunk.hecclient.EventBatch;
import com.splunk.hecclient.PollerCallback;


import java.util.List;

/**
 * Created by kchen on 9/24/17.
 */
public class HecPollerCallback implements PollerCallback {
    private SplunkSinkTask task;

    public HecPollerCallback(SplunkSinkTask task) {
        this.task = task;
    }

    @Override
    public void onEventCommitted(final List<EventBatch> batches) {
        this.task.onEventCommitted(batches);
    }

    @Override
    public void onEventFailure(final List<EventBatch> batches, Exception ex) {
        this.task.onEventFailure(batches, ex);
    }
}