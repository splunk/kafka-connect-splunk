package com.splunk.hecclient;

import java.util.List;

/**
 * Created by kchen on 10/18/17.
 */


// The implementation of PollerCallback shall be threadsafe since the callback may be invoked in multiple threads
public interface PollerCallback {
    void onEventFailure(List<EventBatch> failure);
    void onEventCommitted(List<EventBatch> committed);
}
