package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Collections;

import static com.splunk.hecclient.JsonEventBatch.ENDPOINT;

public class HecURIBuilderTest {
    private static final String BASE_URL =  "https://localhost:8088";
    private static final String TOKEN =  "mytoken";

    @Test
    public void testDefaultValues() {
        HecConfig hecConfig = new HecConfig(Collections.emptyList(), TOKEN);
        HecURIBuilder builder = new HecURIBuilder(BASE_URL, hecConfig);

        URI uri = builder.getURI(ENDPOINT);

        Assert.assertEquals("https://localhost:8088/services/collector/event", uri.toString());
    }

    @Test
    public void testAutoExtractTimestamp() {
        {
            HecConfig hecConfig = new HecConfig(Collections.emptyList(), TOKEN)
                    .setAutoExtractTimestamp(true);
            HecURIBuilder builder = new HecURIBuilder(BASE_URL, hecConfig);

            URI uri = builder.getURI(ENDPOINT);

            Assert.assertEquals("https://localhost:8088/services/collector/event?" +
                            HecURIBuilder.AUTO_EXTRACT_TIMESTAMP_PARAMETER + "=true",
                    uri.toString());
        }
        {
            HecConfig hecConfig = new HecConfig(Collections.emptyList(), TOKEN)
                    .setAutoExtractTimestamp(false);
            HecURIBuilder builder = new HecURIBuilder(BASE_URL, hecConfig);

            URI uri = builder.getURI(ENDPOINT);

            Assert.assertEquals("https://localhost:8088/services/collector/event?" +
                            HecURIBuilder.AUTO_EXTRACT_TIMESTAMP_PARAMETER + "=false",
                    uri.toString());
        }
    }
}