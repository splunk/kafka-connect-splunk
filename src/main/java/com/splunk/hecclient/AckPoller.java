package com.splunk.hecclient;

import java.util.List;

/**
 * Created by kchen on 10/18/17.
 */
public interface AckPoller {
    void start();
    void stop();
    void add(HecChannel channel, EventBatch batch);
}
