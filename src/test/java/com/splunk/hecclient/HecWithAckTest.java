package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kchen on 10/31/17.
 */
public class HecWithAckTest {
    @Test
    public void create() {
        HecConfig config = UnitUtil.createHecConfig();
        Hec hec = new HecWithAck(config, null);
        Assert.assertNotNull(hec);

        hec = new HecWithAck(config, Hec.createHttpClient(config), (PollerCallback) null);
        Assert.assertNotNull(hec);

        PollerMock pm = new PollerMock(null);
        hec = new HecWithAck(config, Hec.createHttpClient(config), pm);
        Assert.assertNotNull(hec);
        Assert.assertTrue(pm.isStarted());

        hec = new HecWithAck(config, null, new LoadBalancerMock());
        Assert.assertNotNull(hec);

        pm = new PollerMock(null);
        hec = new HecWithAck(config, Hec.createHttpClient(config), pm, new LoadBalancerMock());
        Assert.assertNotNull(hec);
        Assert.assertTrue(pm.isStarted());
    }

    @Test
    public void sendEmptyBatch() {
        LoadBalancerMock lb = new LoadBalancerMock();
        HecConfig config = UnitUtil.createHecConfig();
        Poller pm = new PollerMock(null);
        Hec hec = new HecWithAck(config, Hec.createHttpClient(config), pm, lb);
        boolean result = hec.send(new JsonEventBatch());
        Assert.assertFalse(result);
        Assert.assertEquals(0, lb.getBatches().size());
    }

    @Test
    public void send() {
        LoadBalancerMock lb = new LoadBalancerMock();
        HecConfig config = UnitUtil.createHecConfig();
        Poller pm = new PollerMock(null);
        Hec hec = new HecWithAck(config, Hec.createHttpClient(config), pm, lb);
        boolean result = hec.send(UnitUtil.createBatch());
        Assert.assertTrue(result);
        Assert.assertEquals(1, lb.getBatches().size());
    }

    @Test
    public void close() {
        LoadBalancerMock lb = new LoadBalancerMock();
        HecConfig config = UnitUtil.createHecConfig();
        PollerMock pm = new PollerMock(null);
        Hec hec = new HecWithAck(config, Hec.createHttpClient(config), pm, lb);
        Assert.assertTrue(pm.isStarted());

        hec.close();
        Assert.assertFalse(pm.isStarted());
    }
}
