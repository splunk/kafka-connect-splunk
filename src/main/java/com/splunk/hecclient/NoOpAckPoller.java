package com.splunk.hecclient;

/**
 * Created by kchen on 10/18/17.
 */
public class NoOpAckPoller implements AckPoller {
    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void add(HecChannel channel, EventBatch batch) {
    }
}
