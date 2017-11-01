package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kchen on 10/31/17.
 */
public class ResponsePollerTest {
    @Test
    public void start() {
        ResponsePoller poller = new ResponsePoller(null);
        poller.start();
    }

    @Test
    public void stop() {
        ResponsePoller poller = new ResponsePoller(null);
        poller.stop();
    }

    @Test
    public void fail() {
        PollerCallbackMock cb = new PollerCallbackMock();
        ResponsePoller poller = new ResponsePoller(cb);
        EventBatch batch = UnitUtil.createBatch();
        poller.fail(null, batch, null);
        Assert.assertTrue(batch.isFailed());
        Assert.assertEquals(1, cb.getFailed().size());
    }

    @Test
    public void getTotalOutstandingEventBatches() {
        ResponsePoller poller = new ResponsePoller(null);
        Assert.assertEquals(0, poller.getTotalOutstandingEventBatches());
    }

    @Test
    public void getMinLoadChannel() {
        ResponsePoller poller = new ResponsePoller(null);
        Assert.assertNull(poller.getMinLoadChannel());
    }

    @Test
    public void addFailedBatch() {
        PollerCallbackMock cb = new PollerCallbackMock();
        ResponsePoller poller = new ResponsePoller(cb);
        EventBatch batch = UnitUtil.createBatch();
        String resp = "{\"text\":\"Failed\",\"code\":-10}";
        poller.add(null, batch, resp);
        Assert.assertTrue(batch.isFailed());
        Assert.assertEquals(1, cb.getFailed().size());

        // without callback
        poller = new ResponsePoller(null);
        batch = UnitUtil.createBatch();
        resp = "{\"text\":\"Failed\",\"code\":-10}";
        poller.add(null, batch, resp);
        Assert.assertTrue(batch.isFailed());
    }

    @Test
    public void addSuccessfulBatch() {
        PollerCallbackMock cb = new PollerCallbackMock();
        ResponsePoller poller = new ResponsePoller(cb);

        EventBatch batch = UnitUtil.createBatch();
        String resp = "{\"text\":\"Success\",\"code\":0}";
        poller.add(null, batch, resp);
        Assert.assertTrue(batch.isCommitted());
        Assert.assertEquals(1, cb.getCommitted().size());

        // without callback
        poller = new ResponsePoller(null);
        batch = UnitUtil.createBatch();
        resp = "{\"text\":\"Success\",\"code\":0}";
        poller.add(null, batch, resp);
        Assert.assertTrue(batch.isCommitted());
    }
}
