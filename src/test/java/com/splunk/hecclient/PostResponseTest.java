package com.splunk.hecclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by kchen on 10/31/17.
 */
public class PostResponseTest {
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    @Test
    public void isSucceed() {
        PostResponse resp = getResponse(true);
        Assert.assertTrue(resp.isSucceed());

        resp = getResponse(false);
        Assert.assertFalse(resp.isSucceed());
    }

    @Test
    public void getText() {
        PostResponse resp = getResponse(true);
        Assert.assertEquals("Success", resp.getText());
    }

    @Test
    public void getAckId() {
        PostResponse resp = getResponse(true);
        Assert.assertEquals(7, resp.getAckId());
    }

    @Test
    public void getterSetter() {
        PostResponse resp = new PostResponse();
        resp.setCode(0);
        Assert.assertTrue(resp.isSucceed());

        resp.setText("Failed");
        Assert.assertEquals("Failed", resp.getText());

        resp.setAckId(100);
        Assert.assertEquals(100, resp.getAckId());
    }

    private PostResponse getResponse(boolean success) {
        String resp;
        if (success) {
            resp = "{\"text\":\"Success\",\"code\":0,\"ackId\":7}";
        } else {
            resp = "{\"text\":\"Failed\",\"code\":-10}";
        }

        try {
            return jsonMapper.readValue(resp, PostResponse.class);
        } catch (IOException ex) {
            Assert.assertTrue("failed to deserialize from acks", false);
            throw new HecClientException("failed to deserialize from acks", ex);
        }
    }
}
