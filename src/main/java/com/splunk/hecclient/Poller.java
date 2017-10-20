package com.splunk.hecclient;

import java.util.List;

/**
 * Created by kchen on 10/18/17.
 */
public interface Poller {
    void start();
    void stop();
    void add(HecChannel channel, EventBatch batch, String response);
    void fail(HecChannel channel, EventBatch batch);

    // minimum load channel
    HecChannel getMinLoadChannel();
    long getTotalOutstandingEventBatches();
}
