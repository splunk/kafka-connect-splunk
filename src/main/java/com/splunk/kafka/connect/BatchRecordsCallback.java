package com.splunk.kafka.connect;

import com.splunk.cloudfwd.ConnectionCallbacks;
import com.splunk.cloudfwd.EventBatch;

/**
 * Created by kchen on 9/24/17.
 */
public class BatchRecordsCallback implements ConnectionCallbacks {
    public BatchRecordsCallback() {
    }

    @Override
    public void acknowledged(EventBatch events) {
    }

    @Override
    public void failed(EventBatch events, Exception ex) {
    }

    @Override
    public void checkpoint(EventBatch events) {
        // long sequenceNumber = (long) events.getId(); // highest sequence number in the event batch
        // FIXME Commits checkpoint
    }
}
