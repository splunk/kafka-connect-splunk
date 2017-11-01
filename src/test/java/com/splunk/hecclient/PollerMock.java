package com.splunk.hecclient;

/**
 * Created by kchen on 10/31/17.
 */
public class PollerMock implements Poller {
    private PollerCallback callback;
    private boolean started;

    public PollerMock(PollerCallback callback) {
        this.callback = callback;
    }

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
    }

    public boolean isStarted() {
        return started;
    }
}
