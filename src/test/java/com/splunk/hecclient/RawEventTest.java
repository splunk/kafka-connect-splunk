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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RawEventTest {
    static final ObjectMapper jsonMapper = new ObjectMapper();
    static final String separator = "###";

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

    @Test
    public void struct() {
        final Schema childSchema = SchemaBuilder.struct()
            .name("child")
            .field("first_name", Schema.STRING_SCHEMA)
            .field("age", Schema.INT32_SCHEMA)
            .build();
        final Schema parentSchema = SchemaBuilder.struct()
            .name("test")
            .field("first_name", Schema.STRING_SCHEMA)
            .field("count", Schema.INT32_SCHEMA)
            .field("timestamp", Timestamp.SCHEMA)
            .field("children", SchemaBuilder.array(childSchema).build())
            .build();
        final List<Struct> children = Arrays.asList(
            new Struct(childSchema).put("first_name", "Thing 1").put("age", 4),
            new Struct(childSchema).put("first_name","Thing 2").put("age", 7)
        );
        final Struct struct = new Struct(parentSchema)
            .put("first_name", "fred")
            .put("count", 1234)
            .put("timestamp", new Date(1524838717123L))
            .put("children", children);
        final Event event = new RawEvent(struct, null);
        final String expected = "{\"first_name\":\"fred\",\"count\":1234,\"timestamp\":\"2018-04-27T14:18:37.123+0000\",\"children\":[{\"first_name\":\"Thing 1\",\"age\":4},{\"first_name\":\"Thing 2\",\"age\":7}]}";
        final String actual = event.toString();
        Assert.assertEquals(expected, actual);
    }

    @Test(expected = HecException.class)
    public void createInvalidRawEventWithNullData() {
        Event event = new RawEvent(null, null);
    }

    @Test(expected = HecException.class)
    public void createInvalidRawEventWithEmptyString() {
        Event event = new RawEvent("", null);
    }

    @Test
    public void getBytes() {
        // String payload
        Event event = new RawEvent("ni", null);
        for (int i = 0; i < 2; i++) {
            byte[] data = event.getBytes();
            Assert.assertNotNull(data);
            try {
                String got = new String(data, "UTF-8");
                Assert.assertEquals(got, "ni");
            } catch (UnsupportedEncodingException ex) {
                Assert.assertFalse("failed to get string out of byte", true);
                throw new HecException("failed to get string out of byte", ex);
            }
        }

        // byte payload
        byte[] bytes = new byte[2];
        bytes[0] = 'n';
        bytes[1] = 'i';
        event = new RawEvent(bytes, null);
        for (int i = 0; i < 1; i++) {
            byte[] data = event.getBytes();
            Assert.assertArrayEquals(data, bytes);
        }

        // jso object
        Map<String, String> m = new HashMap<>();
        m.put("hello", "world");
        event = new RawEvent(m, null);
        for (int i = 0; i < 2; i++) {
            byte[] data = event.getBytes();
            Assert.assertNotNull(data);
            try {
                HashMap<?, ?> map = (HashMap<?, ?>) jsonMapper.readValue(data, HashMap.class);
                Assert.assertEquals(map.get("hello"), "world");
            } catch (IOException ex) {
                Assert.assertFalse("expect no exception but got exception", false);
                throw new HecException("failed to parse raw event", ex);
            }
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
            throw new HecException("failed to parse raw event", ex);
        }
    }

    @Test
    public void getInputStreamWithoutLineBreaker() {
        getInputStream(null);
    }

    @Test
    public void getInputStreamWithLineBreaker() {
        getInputStream(separator);
    }

    private void getInputStream(final String lineBreaker) {
        String e = "ni";
        RawEvent event = new RawEvent(e, "hao");
        event.setLineBreaker(lineBreaker);
        InputStream stream = event.getInputStream();
        byte[] data = new byte[1024];
        int siz = UnitUtil.read(stream, data);
        if (lineBreaker != null) {
            Assert.assertEquals(siz, e.length() + lineBreaker.length());
        } else {
            Assert.assertEquals(siz, e.length());
        }

        String got = new String(data, 0, siz);

        if (lineBreaker != null) {
            Assert.assertEquals(e + lineBreaker, got);
        } else {
            Assert.assertEquals(e, got);
        }
    }

    @Test
    public void writeToWithoutCarriageReturn() {
        writeTo(null);
    }

    @Test
    public void writeToWithCarriageReturn() {
        writeTo(separator);
    }

    private void writeTo(final String lineBreaker) {
        String eventData = "ni";
        RawEvent e = new RawEvent(eventData, null);
        e.setLineBreaker(lineBreaker);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            e.writeTo(stream);
        } catch (IOException ex) {
            Assert.assertTrue("failed to write to stream", false);
            throw new HecException("failed to write to stream", ex);
        }

        String dataGot = stream.toString();
        if (lineBreaker != null) {
            Assert.assertEquals(eventData + lineBreaker, dataGot);
        } else {
            Assert.assertEquals(eventData, dataGot);
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
        RawEvent event = new RawEvent(data, null);
        Assert.assertEquals(event.length(), data.length());

        data = "ni";
        event = new RawEvent(data, null);
        event.setLineBreaker(separator);
        Assert.assertEquals(data.length() + separator.length(), event.length());
    }
}
