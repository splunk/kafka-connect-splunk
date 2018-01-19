/*
 * Copyright 2017 Splunk, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

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
