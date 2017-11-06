package com.splunk.kafka.connect;

import com.splunk.hecclient.EventBatch;
import com.splunk.hecclient.HecException;
import com.splunk.hecclient.HecInf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by kchen on 11/2/17.
 */
public class HecMock implements HecInf {
    static final String success = "success";
    static final String successAndThenFailure = "successAndThenFailure";
    static final String failure = "failure";

    private List<EventBatch> batches;
    private SplunkSinkTask task;
    private String sentResult = "success";

    public HecMock(SplunkSinkTask task) {
        this.task = task;
        this.batches = new ArrayList<>();
    }

    @Override
    public void close() {
    }

    @Override
    public void send(final EventBatch batch) {
        batches.add(batch);
        if (sentResult.equals(success)) {
            batch.commit();
            task.onEventCommitted(Arrays.asList(batch));
        } else if (sentResult.equals(failure)) {
            batch.fail();
            task.onEventFailure(Arrays.asList(batch), new HecException("mockup"));
        } else {
            batch.fail();
            task.onEventFailure(Arrays.asList(batch), new HecException("mockup"));
        }
    }

    public void setSendReturnResult(final String result) {
        sentResult = result;
    }

    public List<EventBatch> getBatches() {
        return batches;
    }
}
