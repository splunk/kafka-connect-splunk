package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by kchen on 11/2/17.
 */
public class HecAckPollerTest {
    @Test
    public void getterSetter() {
        HecAckPoller poller = new HecAckPoller(null);
        poller.setAckPollInterval(1);
        Assert.assertEquals(1, poller.getAckPollInterval());

        poller.setAckPollThreads(2);
        Assert.assertEquals(2, poller.getAckPollThreads());

        poller.setEventBatchTimeout(3);
        Assert.assertEquals(3, poller.getEventBatchTimeout());
    }

    @Test
    public void startStop() {
        HecAckPoller poller = new HecAckPoller(null);
        Assert.assertFalse(poller.isStarted());

        poller.start();
        Assert.assertTrue(poller.isStarted());

        poller.start();
        Assert.assertTrue(poller.isStarted());

        poller.stop();
        Assert.assertFalse(poller.isStarted());

        poller.stop();
        Assert.assertFalse(poller.isStarted());
    }

    @Test
    public void fail() {
        // with callback
        PollerCallbackMock cb = new PollerCallbackMock();
        HecAckPoller poller = new HecAckPoller(cb);
        IndexerMock indexer = new IndexerMock();
        HecChannel ch = new HecChannel(indexer);
        EventBatch batch = UnitUtil.createBatch();

        poller.fail(ch, batch, new HecException("mockedup"));
        Assert.assertTrue(batch.isFailed());

        Assert.assertEquals(1, cb.getFailed().size());
        Assert.assertEquals(0, cb.getCommitted().size());

        // without callback
        poller = new HecAckPoller(cb);
        indexer = new IndexerMock();
        ch = new HecChannel(indexer);
        batch = UnitUtil.createBatch();

        poller.fail(ch, batch, new HecException("mockedup"));
        Assert.assertTrue(batch.isFailed());
    }

    @Test
    public void add() {
        PollerCallbackMock cb = new PollerCallbackMock();
        HecAckPoller poller = new HecAckPoller(cb);
        poller.setAckPollThreads(1);
        poller.setAckPollInterval(2);
        poller.start();

        IndexerMock indexer = new IndexerMock();
        String ackResponse = "{\"acks\":{\"1\":true}}";
        indexer.setResponse(ackResponse);

        HecChannel ch = new HecChannel(indexer);
        EventBatch batch = UnitUtil.createBatch();

        String response = "{\"text\":\"Success\",\"code\":0,\"ackId\":1}";
        poller.add(ch, batch, response);
        long outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(1, outstanding);
        UnitUtil.milliSleep(3000);

        outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(0, outstanding);
        Assert.assertTrue(batch.isCommitted());
        Assert.assertEquals(1, cb.getCommitted().size());
        Assert.assertEquals(0, cb.getFailed().size());

        poller.stop();
    }

    @Test
    public void addWithoutCallback() {
        HecAckPoller poller = new HecAckPoller(null);
        poller.setAckPollThreads(1);
        poller.setAckPollInterval(2);
        poller.start();

        IndexerMock indexer = new IndexerMock();
        String ackResponse = "{\"acks\":{\"1\":true}}";
        indexer.setResponse(ackResponse);

        HecChannel ch = new HecChannel(indexer);
        EventBatch batch = UnitUtil.createBatch();

        String response = "{\"text\":\"Success\",\"code\":0,\"ackId\":1}";
        poller.add(ch, batch, response);
        long outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(1, outstanding);
        UnitUtil.milliSleep(3000);

        outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(0, outstanding);
        Assert.assertTrue(batch.isCommitted());

        poller.stop();
    }

    @Test
    public void addWithNoAckReady() {
        PollerCallbackMock cb = new PollerCallbackMock();
        HecAckPoller poller = new HecAckPoller(cb);
        poller.setAckPollThreads(1);
        poller.setAckPollInterval(2);
        poller.start();

        IndexerMock indexer = new IndexerMock();
        String ackResponse = "{\"acks\":{\"1\":false}}";
        indexer.setResponse(ackResponse);

        HecChannel ch = new HecChannel(indexer);
        EventBatch batch = UnitUtil.createBatch();

        String response = "{\"text\":\"Success\",\"code\":0,\"ackId\":1}";
        poller.add(ch, batch, response);
        long outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(1, outstanding);
        UnitUtil.milliSleep(3000);

        outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(1, outstanding);
        Assert.assertFalse(batch.isCommitted());
        Assert.assertEquals(0, cb.getCommitted().size());
        Assert.assertEquals(0, cb.getFailed().size());

        poller.stop();
    }

    @Test
    public void addWithTimeoutError() {
        PollerCallbackMock cb = new PollerCallbackMock();
        HecAckPoller poller = new HecAckPoller(cb);
        poller.setAckPollThreads(1);
        poller.setAckPollInterval(2);
        poller.setEventBatchTimeout(3);
        poller.start();

        IndexerMock indexer = new IndexerMock();
        String ackResponse = "{\"acks\":{\"1\":false}}";
        indexer.setResponse(ackResponse);

        HecChannel ch = new HecChannel(indexer);
        EventBatch batch = UnitUtil.createBatch();

        String response = "{\"text\":\"Success\",\"code\":0,\"ackId\":1}";
        poller.add(ch, batch, response);
        long outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(1, outstanding);
        UnitUtil.milliSleep(5000);

        outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(0, outstanding);
        Assert.assertEquals(1, cb.getFailed().size());
        Assert.assertEquals(0, cb.getCommitted().size());

        poller.stop();
    }

    @Test
    public void addWithWrongAckId() {
        PollerCallbackMock cb = new PollerCallbackMock();
        HecAckPoller poller = new HecAckPoller(cb);
        poller.setAckPollThreads(1);
        poller.setAckPollInterval(2);
        poller.start();

        IndexerMock indexer = new IndexerMock();
        String ackResponse = "{\"acks\":{\"2\":true}}";
        indexer.setResponse(ackResponse);

        HecChannel ch = new HecChannel(indexer);
        EventBatch batch = UnitUtil.createBatch();

        String response = "{\"text\":\"Success\",\"code\":0,\"ackId\":1}";
        poller.add(ch, batch, response);
        long outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(1, outstanding);
        UnitUtil.milliSleep(3000);

        outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(1, outstanding);
        Assert.assertFalse(batch.isCommitted());
        Assert.assertEquals(0, cb.getCommitted().size());
        Assert.assertEquals(0, cb.getFailed().size());

        poller.stop();
    }

    @Test
    public void addWithDupId() {
        HecAckPoller poller = new HecAckPoller(null);
        poller.setAckPollThreads(1);
        poller.start();

        IndexerMock indexer = new IndexerMock();
        String ackResponse = "{\"acks\":{\"1\":true}}";
        indexer.setResponse(ackResponse);

        HecChannel ch = new HecChannel(indexer);
        EventBatch batch = UnitUtil.createBatch();

        String response = "{\"text\":\"Success\",\"code\":0,\"ackId\":1}";
        poller.add(ch, batch, response);

        long outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(1, outstanding);

        // introduce dup id
        poller.add(ch, batch, response);
        outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(1, outstanding);

        poller.stop();
    }

    @Test
    public void getMinLoadChannel() {
        HecAckPoller poller = new HecAckPoller(null);
        HecChannel ch = poller.getMinLoadChannel();
        Assert.assertNull(ch);

        PollerCallbackMock cb = new PollerCallbackMock();
        poller = new HecAckPoller(cb);
        poller.setAckPollThreads(1);
        poller.setAckPollInterval(2);
        poller.start();

        // channel 1, 1 outstanding event
        IndexerMock indexer = new IndexerMock();
        String ackResponse = "{\"acks\":{\"1\":false}}";
        indexer.setResponse(ackResponse);

        HecChannel ch1 = new HecChannel(indexer);
        EventBatch batch = UnitUtil.createBatch();

        String response = "{\"text\":\"Success\",\"code\":0,\"ackId\":1}";
        poller.add(ch1, batch, response);

        // channel 2, 2 outstanding events
        indexer = new IndexerMock();
        ackResponse = "{\"acks\":{\"1\":false,\"2\":false}}";
        indexer.setResponse(ackResponse);

        HecChannel ch2 = new HecChannel(indexer);
        batch = UnitUtil.createBatch();
        response = "{\"text\":\"Success\",\"code\":0,\"ackId\":1}";
        poller.add(ch2, batch, response);

        batch = UnitUtil.createBatch();
        response = "{\"text\":\"Success\",\"code\":0,\"ackId\":2}";
        poller.add(ch2, batch, response);

        // channel 3, 3 outstanding events
        indexer = new IndexerMock();
        ackResponse = "{\"acks\":{\"1\":false,\"2\":false, \"3\":false}}";
        indexer.setResponse(ackResponse);

        HecChannel ch3 = new HecChannel(indexer);
        batch = UnitUtil.createBatch();
        response = "{\"text\":\"Success\",\"code\":0,\"ackId\":1}";
        poller.add(ch3, batch, response);

        batch = UnitUtil.createBatch();
        response = "{\"text\":\"Success\",\"code\":0,\"ackId\":2}";
        poller.add(ch3, batch, response);

        batch = UnitUtil.createBatch();
        response = "{\"text\":\"Success\",\"code\":0,\"ackId\":3}";
        poller.add(ch3, batch, response);

        long outstanding = poller.getTotalOutstandingEventBatches();
        Assert.assertEquals(6, outstanding);
        HecChannel minCh = poller.getMinLoadChannel();
        Assert.assertEquals(ch1, minCh);

        poller.stop();
    }
}
