package com.splunk.hecclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by kchen on 12/7/17.
 */
public class DoubleSerializerTest {
    @JsonSerialize(using = DoubleSerializer.class)
    private Double d;

    void setD(Double d) {
        this.d = d;
    }

    @Test
    public void serialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        byte[] bytes = mapper.writeValueAsBytes(this);
        Assert.assertEquals(new String("{\"d\":null}"), new String(bytes));

        d = 10000.0;
        bytes = mapper.writeValueAsBytes(this);
        Assert.assertEquals(new String("{\"d\":10000.000000}"), new String(bytes));

        d = 10000.123456;
        bytes = mapper.writeValueAsBytes(this);
        Assert.assertEquals(new String("{\"d\":10000.123456}"), new String(bytes));

        d = 10000.123456789;
        bytes = mapper.writeValueAsBytes(this);
        Assert.assertEquals(new String("{\"d\":10000.123457}"), new String(bytes));

        d = 10000.123456189;
        bytes = mapper.writeValueAsBytes(this);
        Assert.assertEquals(new String("{\"d\":10000.123456}"), new String(bytes));
    }
}
