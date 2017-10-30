package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kchen on 10/30/17.
 */
public class JsonEventTest {
    @Test
    public void createValidJsonEvent() {
        String data = "this is splunk event";
        Event event = new JsonEvent(data, null);
        Assert.assertEquals(event.getData(), data);
        Assert.assertEquals(event.getTiedObject(), null);

        String tied = "i love you";
        event = new JsonEvent(data, tied);

        Assert.assertEquals(event.getTiedObject(), tied);
        Assert.assertEquals(event.getData(), data);
    }

    @Test(expected = HecClientException.class)
    public void createInvalidJsonEventWithNullData() {
        Event event = new JsonEvent(null, null);
    }

    @Test(expected = HecClientException.class)
    public void createInvalidJsonEventWithEmptyString() {
        Event event = new JsonEvent("", null);
    }
}