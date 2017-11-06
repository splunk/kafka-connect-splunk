package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kchen on 10/31/17.
 */
public class ConcurrentHecTest {
    @Test
    public void create() {
        HecConfig config = UnitUtil.createHecConfig();
        HecInf hec = new ConcurrentHec(1, true, config, null);
        Assert.assertNotNull(hec);
        hec.close();

        hec = new ConcurrentHec(1, false, config, null, new LoadBalancerMock());
        Assert.assertNotNull(hec);
        hec.close();
        hec.close();
    }

    @Test
    public void send() {
        HecConfig config = UnitUtil.createHecConfig();
        LoadBalancerMock lb = new LoadBalancerMock();
        HecInf hec = new ConcurrentHec(1, true, config,null, lb);
        hec.send(UnitUtil.createBatch());
        UnitUtil.milliSleep(20);
        Assert.assertEquals(1, lb.getBatches().size());
        Assert.assertEquals(1, lb.getBatches().get(0).getEvents().size());
        Assert.assertEquals("ni", lb.getBatches().get(0).getEvents().get(0).getEvent());
        hec.close();
        hec.close();
    }

    @Test
    public void sendWithFailure() {
        HecConfig config = UnitUtil.createHecConfig();
        LoadBalancerMock lb = new LoadBalancerMock();
        PollerCallbackMock poller = new PollerCallbackMock();
        lb.setThrowOnSend(true);
        HecInf hec = new ConcurrentHec(1, true, config, poller, lb);
        hec.send(UnitUtil.createBatch());
        UnitUtil.milliSleep(20);
        Assert.assertEquals(0, lb.getBatches().size());
        Assert.assertEquals(1, poller.getFailed().size());
        Assert.assertTrue(poller.getFailed().get(0).isFailed());
        Assert.assertEquals(1, poller.getFailed().get(0).getFailureCount());
        hec.close();
    }
}
