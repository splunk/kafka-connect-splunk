package com.splunk.hecclient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kchen on 10/31/17.
 */
public class LoadBalancerMock implements LoadBalancerInf {
    private List<EventBatch> batches = new ArrayList<>();

    public void add(HecChannel channel) {
    }

    public void remove(HecChannel channel) {
    }

    public boolean send(final EventBatch batch) {
        batches.add(batch);
        return true;
    }

    public int size() {
        return 0;
    }

    public List<EventBatch> getBatches() {
        return batches;
    }
}
