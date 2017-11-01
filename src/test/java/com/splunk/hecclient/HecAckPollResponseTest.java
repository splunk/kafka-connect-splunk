package com.splunk.hecclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created by kchen on 10/31/17.
 */
public class HecAckPollResponseTest {
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    @Test
    public void getSuccessIds() {
        HecAckPollResponse resp = getResposne();
        Collection<Long> succeed = resp.getSuccessIds();
        Assert.assertEquals(3, succeed.size());
        for (int i = 1; i < 4; i++) {
            Assert.assertTrue(succeed.contains(new Long(i)));
        }
    }

    @Test
    public void getAcks() {
        HecAckPollResponse resp = getResposne();
        Map<String, Boolean> acks = resp.getAcks();
        Assert.assertEquals(6, acks.size());
        for (int i = 1; i < 4; i++) {
            Assert.assertEquals(acks.get(String.valueOf(i)), true);
        }

        for (int i = 4; i < 7; i++) {
            Assert.assertEquals(acks.get(String.valueOf(i)), false);
        }
    }

    private HecAckPollResponse getResposne() {
        String acks = "{\"acks\":{\"1\":true,\"2\":true,\"3\":true,\"4\":false,\"5\":false,\"6\":false}}";
        try {
            return jsonMapper.readValue(acks, HecAckPollResponse.class);
        } catch (IOException ex) {
            Assert.assertTrue("failed to deserialize from acks", false);
            throw new HecClientException("failed to deserialize from acks", ex);
        }
    }
}
