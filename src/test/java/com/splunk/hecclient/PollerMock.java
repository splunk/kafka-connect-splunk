package com.splunk.hecclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kchen on 10/31/17.
 */
public class PollerMock implements Poller {
    private boolean started;
    private HecChannel channel;
    private EventBatch batch;
    private EventBatch failedBatch;
    private String response;
    private Exception exception;

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public void fail(HecChannel channel, EventBatch batch, Exception ex) {
        this.channel = channel;
        this.failedBatch = batch;
        this.exception = ex;
    }

    @Override
    public long getTotalOutstandingEventBatches() {
        return 0;
    }

    @Override
    public HecChannel getMinLoadChannel() {
        return null;
    }

    @Override
    public void add(HecChannel channel, EventBatch batch, String resp) {
        this.channel = channel;
        this.batch = batch;
        this.response = resp;
    }

    public boolean isStarted() {
        return started;
    }

    public HecChannel getChannel() {
        return channel;
    }

    public EventBatch getBatch() {
        return batch;
    }

    public EventBatch getFailedBatch() {
        return failedBatch;
    }

    public Exception getException() {
        return exception;
    }

    public String getResponse() {
        return response;
    }
}
