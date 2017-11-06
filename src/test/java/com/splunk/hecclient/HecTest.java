package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kchen on 10/31/17.
 */
public class HecTest {
    @Test
    public void create() {
        HecConfig config = UnitUtil.createHecConfig();
        Hec hec = Hec.newHecWithAck(config, null);
        Assert.assertNotNull(hec);

        hec = Hec.newHecWithAck(config, Hec.createHttpClient(config), (PollerCallback) null);
        Assert.assertNotNull(hec);

        hec = Hec.newHecWithAck(config, null, new LoadBalancerMock());
        Assert.assertNotNull(hec);

        hec = Hec.newHecWithoutAck(config, null);
        Assert.assertNotNull(hec);

        hec = Hec.newHecWithoutAck(config, Hec.createHttpClient(config), (PollerCallback) null);
        Assert.assertNotNull(hec);

        hec = Hec.newHecWithoutAck(config, null, new LoadBalancerMock());
        Assert.assertNotNull(hec);
    }

    @Test
    public void sendEmptyBatch() {
        LoadBalancerMock lb = new LoadBalancerMock();
        HecConfig config = UnitUtil.createHecConfig();
        Poller pm = new PollerMock();
        Hec hec = new Hec(config, Hec.createHttpClient(config), pm, lb);
        hec.send(new JsonEventBatch());
        Assert.assertEquals(0, lb.getBatches().size());
    }

    @Test
    public void send() {
        LoadBalancerMock lb = new LoadBalancerMock();
        HecConfig config = UnitUtil.createHecConfig();
        Poller pm = new PollerMock();
        Hec hec = new Hec(config, Hec.createHttpClient(config), pm, lb);
        hec.send(UnitUtil.createBatch());
        Assert.assertEquals(1, lb.getBatches().size());
    }

    @Test
    public void close() {
        LoadBalancerMock lb = new LoadBalancerMock();
        HecConfig config = UnitUtil.createHecConfig();
        PollerMock pm = new PollerMock();
        Hec hec = new Hec(config, Hec.createHttpClient(config), pm, lb);
        Assert.assertTrue(pm.isStarted());

        hec.close();
        Assert.assertFalse(pm.isStarted());
    }
}
