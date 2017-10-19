package com.splunk.hecclient;

import java.util.List;

/**
 * Created by kchen on 10/18/17.
 */


// The implementation of AckCallback shall be threadsafe since the callback may be invoked in multiple threads
public interface AckCallback {
    void onEventLost(List<EventBatch> lost);
    void onEventAcked(List<EventBatch> acked);
}
