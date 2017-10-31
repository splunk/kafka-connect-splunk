package com.splunk.hecclient;

import org.apache.http.HttpEntity;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by kchen on 10/31/17.
 */
public class JsonEvenBatchTest {
    @Test
    public void add() {
        Event event = new JsonEvent("ni", "hao");
        EventBatch batch = new JsonEventBatch();
        batch.add(event);
        List<Event> events = batch.getEvents();
        Assert.assertEquals(events.size(), 1);
        Event eventGot = events.get(0);
        Assert.assertEquals(event.getEvent(), eventGot.getEvent());
        Assert.assertEquals(event.getTied(), eventGot.getTied());
    }

    @Test(expected = HecClientException.class)
    public void addWithFailure() {
        Event event = new RawEvent("ni", "hao");
        EventBatch batch = new JsonEventBatch();
        batch.add(event);
    }

    @Test
    public void getRestEndpoint() {
        EventBatch batch = new JsonEventBatch();
        Assert.assertEquals(batch.getRestEndpoint(), JsonEventBatch.endpoint);
    }

    @Test
    public void getContentType() {
        EventBatch batch = new JsonEventBatch();
        Assert.assertEquals(batch.getContentType(), JsonEventBatch.contentType);
    }

    @Test
    public void createFromThis() {
        EventBatch batch = new JsonEventBatch();
        EventBatch jsonBatch = batch.createFromThis();
        Assert.assertNotNull(jsonBatch);
        Assert.assertTrue(jsonBatch instanceof JsonEventBatch);
    }

    @Test
    public void isTimedout() {
        EventBatch batch = new JsonEventBatch();
        batch.resetSendTimestamp();
        Assert.assertFalse(batch.isTimedout(1));
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException ex) {
        }

        Assert.assertTrue(batch.isTimedout(1));

        // reset timestamp
        batch.resetSendTimestamp();
        Assert.assertFalse(batch.isTimedout(1));
    }

    @Test
    public void setterGetter() {
        EventBatch batch = new JsonEventBatch();
        Assert.assertTrue(batch.isEmpty());
        Assert.assertEquals(batch.length(), 0);
        Assert.assertEquals(batch.size(), 0);
        Assert.assertTrue(batch.isEmpty());
        Assert.assertFalse(batch.isCommitted());
        Assert.assertEquals(batch.getFailureCount(), 0);

        batch.init();
        Assert.assertFalse(batch.isFailed());
        Assert.assertFalse(batch.isCommitted());

        batch.fail();
        Assert.assertTrue(batch.isFailed());
        Assert.assertEquals(batch.getFailureCount(), 1);

        batch.commit();
        Assert.assertTrue(batch.isCommitted());

        Event event = new JsonEvent("ni", "hao");
        batch.add(event);
        String data = "{\"event\":\"ni\"}";
        Assert.assertEquals(data.length() + 1, batch.length());
        Assert.assertEquals(1, batch.size());
        Assert.assertFalse(batch.isEmpty());

        List<Event> events = batch.getEvents();
        Assert.assertEquals(1, events.size());

        // Add extra fields
        Map<String, String> fields = new HashMap<>();
        fields.put("hello", "world");
        batch.addExtraFields(fields);

        Assert.assertEquals(fields, event.getFields());
    }

    @Test
    public void toStr() {
        EventBatch batch = new JsonEventBatch();
        String str = batch.toString();
        Assert.assertEquals("[]", str);

        Event event = new JsonEvent("ni", "hao");
        batch.add(event);
        str = batch.toString();
        Assert.assertEquals(str, "[{\"event\":\"ni\"},]");
    }

    @Test
    public void getHttpEntity() {
        EventBatch batch = new JsonEventBatch();
        HttpEntity entity = batch.getHttpEntity();
        Assert.assertTrue(entity.isRepeatable());
        Assert.assertFalse(entity.isStreaming());
        Assert.assertEquals(0, entity.getContentLength());

        Event event = new JsonEvent("ni", "hao");
        batch.add(event);

        entity = batch.getHttpEntity();
        Assert.assertEquals(event.length(), entity.getContentLength());

        InputStream stream;
        try {
            stream = entity.getContent();
        } catch (IOException ex) {
            Assert.assertTrue("failed to getContent", false);
            throw new HecClientException("failed to getContent", ex);
        }

        byte[] data = new byte[1024];
        int siz = 0;
        while (true) {
            try {
                int read = stream.read(data, siz, data.length - siz);
                if (read < 0) {
                    break;
                }
                siz += read;
            } catch (IOException ex) {
                Assert.assertTrue("failed to read from stream", false);
                throw new HecClientException("failed to read from stream", ex);
            }
        }

        String expected = "{\"event\":\"ni\"}\n";
        Assert.assertEquals(expected, new String(data, 0, siz));
    }
}
