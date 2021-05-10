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

import java.util.List;

public class RawEventBatchTest {
    @Test
    public void add() {
        Event event = new RawEvent("ni", "hao");
        EventBatch batch = RawEventBatch.factory().build();
        batch.add(event);
        List<Event> events = batch.getEvents();
        Assert.assertEquals(events.size(), 1);
        Event eventGot = events.get(0);
        Assert.assertEquals(event.getEvent(), eventGot.getEvent());
        Assert.assertEquals(event.getTied(), eventGot.getTied());
    }

    @Test(expected = HecException.class)
    public void addWithFailure() {
        Event event = new JsonEvent("ni", "hao");
        EventBatch batch = RawEventBatch.factory().build();
        batch.add(event);
    }

    @Test
    public void getRestEndpoint() {
        // Without metadata
        EventBatch batch = RawEventBatch.factory().build();
        Assert.assertEquals(batch.getRestEndpoint(), RawEventBatch.endpoint);

        // With all metadata
        EventBatch rawBatch = RawEventBatch.factory()
                .setHost("localhost")
                .setSource("source")
                .setSourcetype("sourcetype")
                .setTime(1000)
                .setIndex("index")
                .build();
        String endpoint = rawBatch.getRestEndpoint();
        Assert.assertTrue(endpoint.contains("index=index"));
        Assert.assertTrue(endpoint.contains("host=localhost"));
        Assert.assertTrue(endpoint.contains("source=source"));
        Assert.assertTrue(endpoint.contains("sourcetype=sourcetype"));
        Assert.assertTrue(endpoint.contains("time=1000"));

        // With partial metadata
        EventBatch rawBatchPartial  = RawEventBatch.factory()
                .setHost("localhost")
                .setIndex("index")
                .setSource("")
                .build();
        endpoint = rawBatchPartial.getRestEndpoint();
        Assert.assertTrue(endpoint.contains("index=index"));
        Assert.assertTrue(endpoint.contains("host=localhost"));
        Assert.assertFalse(endpoint.contains("source="));
        Assert.assertFalse(endpoint.contains("sourcetype="));
        Assert.assertFalse(endpoint.contains("time="));
    }

    @Test
    public void getContentType() {
        EventBatch batch = RawEventBatch.factory().build();
        Assert.assertEquals(batch.getContentType(), RawEventBatch.contentType);
    }

    @Test
    public void createFromThis() {
        EventBatch batch = RawEventBatch.factory().build();
        EventBatch rawBatch = batch.createFromThis();
        Assert.assertNotNull(rawBatch);
        Assert.assertTrue(rawBatch instanceof RawEventBatch);
    }

    @Test
    public void getter() {
        RawEventBatch batch = RawEventBatch.factory()
                .setSource("source")
                .setIndex("index")
                .setSourcetype("sourcetype")
                .setHost("host")
                .setTime(1)
                .build();
        Assert.assertEquals("source", batch.getSource());
        Assert.assertEquals("sourcetype", batch.getSourcetype());
        Assert.assertEquals("index", batch.getIndex());
        Assert.assertEquals(1, batch.getTime());
    }

    @Test
    public void checkEquals() {
        RawEventBatch batchOne = RawEventBatch.factory()
                .setSource("source3")
                .setIndex("idx1")
                .setSourcetype("sourcetype2")
                .setHost("host4")
                .build();

        RawEventBatch batchTwo = RawEventBatch.factory()
                .setSource("source")
                .setIndex("idx")
                .setSourcetype("1sourcetype2")
                .setHost("3host4")
                .build();

        Assert.assertNotEquals(batchOne, batchTwo);
    }
}
