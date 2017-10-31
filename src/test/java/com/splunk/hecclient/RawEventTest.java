package com.splunk.hecclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kchen on 10/30/17.
 */
public class RawEventTest {
    static final ObjectMapper jsonMapper = new ObjectMapper();

    @Test
    public void createValidRawEvent() {
        String data = "this is splunk event";

        // without tied object
        Event event = new RawEvent(data, null);
        Assert.assertEquals(event.getEvent(), data);
        Assert.assertEquals(event.getTied(), null);

        // with tied object
        String tied = "i love you";
        event = new JsonEvent(data, tied);

        Assert.assertEquals(event.getTied(), tied);
        Assert.assertEquals(event.getEvent(), data);
    }

    @Test(expected = HecClientException.class)
    public void createInvalidRawEventWithNullData() {
        Event event = new RawEvent(null, null);
    }

    @Test(expected = HecClientException.class)
    public void createInvalidRawEventWithEmptyString() {
        Event event = new RawEvent("", null);
    }

    @Test
    public void getBytes() {
        // String payload
        Event event = new RawEvent("ni", null);
        byte[] data = event.getBytes();
        Assert.assertNotNull(data);
        try {
            String got = new String(data, "UTF-8");
            Assert.assertEquals(got, "ni");
        } catch (UnsupportedEncodingException ex) {
            Assert.assertFalse("failed to get string out of byte", true);
            throw new HecClientException("failed to get string out of byte", ex);
        }

        // byte payload
        byte[] bytes = new byte[2];
        bytes[0] = 'n';
        bytes[1] = 'i';

        event = new RawEvent(bytes, null);
        data = event.getBytes();
        Assert.assertNotNull(data);
        Assert.assertEquals(data[0], 'n');
        Assert.assertEquals(data[1], 'i');

        // json object
        Map<String, String> m = new HashMap<>();
        m.put("hello", "world");
        event = new RawEvent(m, null);
        data = event.getBytes();
        Assert.assertNotNull(data);
        try {
            HashMap<?, ?> map = (HashMap<?, ?>) jsonMapper.readValue(data, HashMap.class);
            Assert.assertEquals(map.get("hello"), "world");
        } catch (IOException ex) {
            Assert.assertFalse("expect no exception but got exception", false);
            throw new HecClientException("failed to parse raw event", ex);
        }
    }

    @Test
    public void toStr() {
        // String payload
        Event event = new RawEvent("ni", null);
        String data = event.toString();
        Assert.assertNotNull(data);
        Assert.assertEquals(data, "ni");

        // byte payload
        byte[] bytes = new byte[2];
        bytes[0] = 'n';
        bytes[1] = 'i';

        event = new RawEvent(bytes, null);
        data = event.toString();
        Assert.assertNotNull(data);
        Assert.assertEquals(data, "ni");

        // json object
        Map<String, String> m = new HashMap<>();
        m.put("hello", "world");
        event = new RawEvent(m, null);
        data = event.toString();
        Assert.assertNotNull(data);
        try {
            HashMap<?, ?> map = (HashMap<?, ?>) jsonMapper.readValue(data, HashMap.class);
            Assert.assertEquals(map.get("hello"), "world");
        } catch (IOException ex) {
            Assert.assertFalse("expect no exception but got exception", false);
            throw new HecClientException("failed to parse raw event", ex);
        }
    }

    @Test
    public void getterSetter() {
        Event event = new RawEvent("ni", null);
        Map<String, String> m = new HashMap<String, String>();
        m.put("hello", "world");
        event.setFields(m);
        Assert.assertNull(event.getFields()); // we ignore extra fields for raw event

        event.addFields(m);
        Assert.assertNull(event.getFields()); // we ignore extra fields for raw event
    }

    @Test
    public void length() {
        String data = "ni";
        Event event = new RawEvent(data, null);
        Assert.assertEquals(event.length(), data.length() + 1); // if we don't have trailing "\n", we add it

        data = "ni\n";
        event = new RawEvent(data, null);
        Assert.assertEquals(event.length(), data.length()); // if we have trailing "\n", we don't add "\n"
    }

    @Test
    public void endswith() {
        byte[] data = new byte[2];
        data[0] = 'n';
        data[1] = 'i';

        boolean r = Event.endswith(data, (byte) 'i');
        Assert.assertTrue(r);

        r = Event.endswith(data, (byte) 'n');
        Assert.assertFalse(r);

        data = new byte[0];
        r = Event.endswith(data, (byte) 'i');
        Assert.assertFalse(r);
    }
}