package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

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
        boolean result = hec.send(UnitUtil.createBatch());
        Assert.assertTrue(result);
        UnitUtil.milliSleep(20);
        Assert.assertEquals(1, lb.getBatches().size());
        Assert.assertEquals(1, lb.getBatches().get(0).getEvents().size());
        Assert.assertEquals("ni", lb.getBatches().get(0).getEvents().get(0).getEvent());
        hec.close();
        hec.close();
    }
}
