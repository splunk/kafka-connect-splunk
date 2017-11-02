package com.splunk.hecclient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by kchen on 10/31/17.
 */
public class PollerCallbackMock implements PollerCallback {
    private ConcurrentLinkedQueue<EventBatch> failed = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<EventBatch> committed = new ConcurrentLinkedQueue<>();

    public void onEventFailure(final List<EventBatch> failure, Exception ex) {
        failed.addAll(failure);
    }

    public void onEventCommitted(final List<EventBatch> commit) {
        committed.addAll(commit);
    }

    public List<EventBatch> getFailed() {
        List<EventBatch> results = new ArrayList<>();
        results.addAll(failed);
        return results;
    }

    public List<EventBatch> getCommitted() {
        List<EventBatch> results = new ArrayList<>();
        results.addAll(committed);
        return results;
    }
}
