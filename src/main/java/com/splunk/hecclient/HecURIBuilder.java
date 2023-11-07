package com.splunk.hecclient;

import org.apache.http.client.utils.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

public class HecURIBuilder {
    public static final String AUTO_EXTRACT_TIMESTAMP_PARAMETER = "auto_extract_timestamp";

    private final String baseUrl;
    private final HecConfig hecConfig;

    public HecURIBuilder(String baseUrl, HecConfig hecConfig) {
        this.baseUrl = baseUrl;
        this.hecConfig = hecConfig;
    }

    public URI getURI(String endpoint) {
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl)
                    .setPath(endpoint);

            if (hecConfig.getAutoExtractTimestamp() != null) {
                uriBuilder.addParameter(AUTO_EXTRACT_TIMESTAMP_PARAMETER, hecConfig.getAutoExtractTimestamp().toString());
            }
             return  uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
