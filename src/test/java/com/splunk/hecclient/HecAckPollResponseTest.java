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
import java.util.Collection;
import java.util.Map;

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
            throw new HecException("failed to deserialize from acks", ex);
        }
    }
}
