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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.Assert;
import org.junit.Test;

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
