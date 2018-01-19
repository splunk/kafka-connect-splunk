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
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

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
            throw new HecException("failed to deserialize from acks", ex);
        }
    }
}
