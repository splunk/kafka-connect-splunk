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
