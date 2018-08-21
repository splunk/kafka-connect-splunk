package com.splunk.kafka.connect;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.data.Timestamp;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.splunk.hecclient.Event;
import com.splunk.hecclient.RawEvent;

public class StructEventTest {

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
}
