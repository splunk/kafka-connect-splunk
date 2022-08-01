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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class HecConfigTest {
    @Test
    public void getterSetter() {
        String uri = "https://dummy:8088";
        String token = "mytoken";
        HecConfig config = new HecConfig(Arrays.asList(uri), token);

        List<String> uris = config.getUris();
        Assert.assertEquals(1, uris.size());
        Assert.assertEquals(uri, uris.get(0));
        Assert.assertEquals(token, config.getToken());

        config.setAckPollInterval(1)
              .setDisableSSLCertVerification(true)
              .setHttpKeepAlive(false)
              .setSocketSendBufferSize(2)
              .setSocketTimeout(3)
              .setMaxHttpConnectionPerChannel(4)
              .setTotalChannels(5)
              .setAckPollThreads(6)
              .setEnableChannelTracking(true)
              .setEventBatchTimeout(7)
              .setTrustStorePath("test")
              .setTrustStoreType("PKCS12")
              .setTrustStorePassword("pass")
              .setHasCustomTrustStore(true)
              .setBackoffThresholdSeconds(10)
              .setlbPollInterval(120);

        Assert.assertTrue(config.getDisableSSLCertVerification());
        Assert.assertTrue(config.getEnableChannelTracking());
        Assert.assertFalse(config.getHttpKeepAlive());
        Assert.assertEquals(1, config.getAckPollInterval());
        Assert.assertEquals(2, config.getSocketSendBufferSize());
        Assert.assertEquals(3, config.getSocketTimeout());
        Assert.assertEquals(4, config.getMaxHttpConnectionPerChannel());
        Assert.assertEquals(5, config.getTotalChannels());
        Assert.assertEquals(6, config.getAckPollThreads());
        Assert.assertEquals(7, config.getEventBatchTimeout());
        Assert.assertEquals("test", config.getTrustStorePath());
        Assert.assertEquals("PKCS12", config.getTrustStoreType());
        Assert.assertEquals("pass", config.getTrustStorePassword());
        Assert.assertEquals(10000, config.getBackoffThresholdSeconds());
        Assert.assertEquals(120000, config.getlbPollInterval());
        Assert.assertTrue(config.getHasCustomTrustStore());
    }
}
