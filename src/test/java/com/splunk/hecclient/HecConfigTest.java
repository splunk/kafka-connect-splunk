package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Created by kchen on 10/31/17.
 */
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
                .setEventBatchTimeout(7);

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
    }
}
