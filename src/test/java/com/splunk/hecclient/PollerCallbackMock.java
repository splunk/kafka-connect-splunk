package com.splunk.hecclient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kchen on 10/31/17.
 */
public class PollerCallbackMock implements PollerCallback {
    private List<EventBatch> failed = new ArrayList<>();
    private List<EventBatch> committed = new ArrayList<>();

    public void onEventFailure(final List<EventBatch> failure, Exception ex) {
        failed.addAll(failure);
    }

    public void onEventCommitted(final List<EventBatch> commit) {
        committed.addAll(commit);
    }

    public List<EventBatch> getFailed() {
        return failed;
    }

    public List<EventBatch> getCommitted() {
        return committed;
    }
}
