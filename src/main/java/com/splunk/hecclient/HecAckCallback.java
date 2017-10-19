package com.splunk.hecclient;

import java.util.List;

/**
 * Created by kchen on 10/18/17.
 */
public class HecAckCallback implements AckCallback {
    @Override
    public void onEventLost(List<EventBatch> lost) {
        for (EventBatch batch: lost) {
            System.out.print(batch);
        }
    }

    @Override
    public void onEventAcked(List<EventBatch> acked) {
        for (EventBatch batch: acked) {
            System.out.print(batch);
        }
    }
}
