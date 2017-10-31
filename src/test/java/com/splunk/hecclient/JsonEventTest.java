package com.splunk.hecclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kchen on 10/30/17.
 */
public class JsonEventTest {
    static final ObjectMapper jsonMapper = new ObjectMapper();

    @Test
    public void createValidJsonEvent() {
        String data = "this is splunk event";

        // without tied object
        Event event = new JsonEvent(data, null);
        Assert.assertEquals(event.getEvent(), data);
        Assert.assertEquals(event.getTied(), null);

        // with tied object
        String tied = "i love you";
        event = new JsonEvent(data, tied);

        Assert.assertEquals(event.getTied(), tied);
        Assert.assertEquals(event.getEvent(), data);


    }

    @Test(expected = HecClientException.class)
    public void createInvalidJsonEventWithNullData() {
        Event event = new JsonEvent(null, null);
    }

    @Test(expected = HecClientException.class)
    public void createInvalidJsonEventWithEmptyString() {
        Event event = new JsonEvent("", null);
    }

    @Test
    public void addFields() {
        Event event = new JsonEvent("this is splunk event", null);

        // null extra fields
        event.addFields(null);
        Assert.assertNull(event.getFields());

        // empty extra fields
        Map<String, String> fields = new HashMap<>();
        event.addFields(fields);
        Assert.assertNull(event.getFields());

        // one item
        fields.put("ni", "hao");
        event.addFields(fields);
        Map<String, String> fieldsGot = event.getFields();
        Assert.assertNotNull(fieldsGot);
        Assert.assertEquals(fieldsGot.isEmpty(), false);
        Assert.assertEquals(fieldsGot.size(), 1);
        Assert.assertEquals(fieldsGot.get("ni"), "hao");

        // put another one
        fields.put("hello", "world");
        event.addFields(fields);
        fieldsGot = event.getFields();
        Assert.assertNotNull(fieldsGot);
        Assert.assertEquals(fieldsGot.isEmpty(), false);
        Assert.assertEquals(fieldsGot.size(), 2);
        Assert.assertEquals(fieldsGot.get("ni"), "hao");
        Assert.assertEquals(fieldsGot.get("hello"), "world");
    }

    @Test
    public void toStr() {
        SerialAndDeserial sad = new SerialAndDeserial() {
            @Override
            public Event serializeAndDeserialize(Event event) {
                String stringed = event.toString();
                Assert.assertNotNull(stringed);

                Event deserilized;
                try {
                    deserilized = jsonMapper.readValue(stringed, JsonEvent.class);
                } catch (IOException ex) {
                    Assert.assertFalse("expect no exception but got exception", true);
                    throw new HecClientException("failed to parse JsonEvent", ex);
                }
                return deserilized;
            }
        };
        serialize(sad);
    }

    @Test
    public void getBytes() {
        SerialAndDeserial sad = new SerialAndDeserial() {
            @Override
            public Event serializeAndDeserialize(Event event) {
                byte[] bytes = event.getBytes();
                Assert.assertNotNull(bytes);

                Event deserilized;
                try {
                    deserilized = jsonMapper.readValue(bytes, JsonEvent.class);
                } catch (IOException ex) {
                    Assert.assertFalse("expect no exception but got exception", false);
                    throw new HecClientException("failed to parse JsonEvent", ex);
                }
                return deserilized;
            }
        };
        serialize(sad);
    }

    @Test
    public void getterSetter() {
        Event event = new JsonEvent("hello", "world");
        Assert.assertEquals(event.getEvent(), "hello");
        Assert.assertEquals(event.getTied(), "world");

        Assert.assertNull(event.getIndex());
        event.setIndex("main");
        Assert.assertEquals(event.getIndex(), "main");

        Assert.assertNull(event.getSource());
        event.setSource("source");
        Assert.assertEquals(event.getSource(), "source");

        Assert.assertNull(event.getSourcetype());
        event.setSourcetype("sourcetype");
        Assert.assertEquals(event.getSourcetype(), "sourcetype");

        Assert.assertNull(event.getHost());
        event.setHost("localhost");
        Assert.assertEquals(event.getHost(), "localhost");

        Assert.assertEquals(event.getTime(), -1);
        event.setTime(1);
        Assert.assertEquals(event.getTime(), 1);

        event.setEvent("ni");
        Assert.assertEquals(event.getEvent(), "ni");

        event.setTied("hao");
        Assert.assertEquals(event.getTied(), "hao");
    }

    private interface SerialAndDeserial {
        Event serializeAndDeserialize(final Event event);
    }

    private void serialize(SerialAndDeserial sad) {
        List<Object> eventDataObjs = new ArrayList<>();
        // String object
        eventDataObjs.add("this is splunk event");

        Map<String, String> m = new HashMap<>();
        m.put("hello", "world");

        // Json object
        eventDataObjs.add(m);

        for (Object eventData: eventDataObjs) {
            doSerialize(eventData, sad);
        }
    }

    private void doSerialize(Object data, SerialAndDeserial sad) {
        String tied = "tied";
        Event event = new JsonEvent(data, tied);

        Map<String, String> fields = new HashMap<>();
        fields.put("ni", "hao");
        event.addFields(fields);
        event.setHost("localhost");
        event.setIndex("main");
        event.setSource("test-source");
        event.setSourcetype("test-sourcetype");
        event.setTime(100000000);

        Event deserialized = sad.serializeAndDeserialize(event);

        Assert.assertEquals(deserialized.getEvent(), data);
        Assert.assertNull(deserialized.getTied()); // we ignore tied when serialize Event
        Assert.assertEquals(deserialized.getHost(), "localhost");
        Assert.assertEquals(deserialized.getIndex(), "main");
        Assert.assertEquals(deserialized.getSource(), "test-source");
        Assert.assertEquals(deserialized.getSourcetype(), "test-sourcetype");
        Assert.assertEquals(deserialized.getTime(), 100000000);

        Map<String, String> fieldsGot = deserialized.getFields();
        Assert.assertEquals(fieldsGot.get("ni"), "hao");
    }
}