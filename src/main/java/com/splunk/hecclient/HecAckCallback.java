package com.splunk.hecclient;

import java.util.List;

/**
 * Created by kchen on 10/18/17.
 */
public class HecAckCallback implements PollerCallback {
    @Override
    public void onEventFailure(List<EventBatch> failure) {
        for (EventBatch batch: failure) {
            System.out.print(batch);
        }
    }

    @Override
    public void onEventCommitted(List<EventBatch> committed) {
        for (EventBatch batch: committed) {
            System.out.print(batch);
        }
    }
}
