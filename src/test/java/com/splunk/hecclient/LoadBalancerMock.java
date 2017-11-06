package com.splunk.hecclient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kchen on 10/31/17.
 */
public class LoadBalancerMock implements LoadBalancerInf {
    private List<EventBatch> batches = new ArrayList<>();
    private boolean throwOnSend = false;

    public void add(HecChannel channel) {
    }

    public void remove(HecChannel channel) {
    }

    public void send(final EventBatch batch) {
        if (throwOnSend) {
            throw new HecException("mocked up");
        }
        batches.add(batch);
    }

    public LoadBalancerMock setThrowOnSend(boolean throwOnSend) {
        this.throwOnSend = throwOnSend;
        return this;
    }

    public int size() {
        return 0;
    }

    public List<EventBatch> getBatches() {
        return batches;
    }
}
