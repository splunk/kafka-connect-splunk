package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by kchen on 10/31/17.
 */
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

    @Test(expected = HecClientException.class)
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
}
