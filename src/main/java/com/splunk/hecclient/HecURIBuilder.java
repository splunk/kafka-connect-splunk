package com.splunk.hecclient;

import org.apache.http.Consts;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;

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
            URIBuilder uriBuilder = new URIBuilder(baseUrl);
            int idx = endpoint.indexOf('?');
            if (idx == -1) {
                // json endpoint
                uriBuilder = uriBuilder.setPath(endpoint);
            } else {
                // in case of raw endpoint, the endpoint will be in form "/services/collector/raw?index=xxx&source=xxx"
                // extract the path and params via a split on '?'
                uriBuilder = uriBuilder.setPath(endpoint.substring(0, idx));
                uriBuilder = uriBuilder.setParameters(URLEncodedUtils.parse(endpoint.substring(idx+1), Consts.UTF_8));
            }
            
            if (hecConfig.getAutoExtractTimestamp() != null) {
                uriBuilder.addParameter(AUTO_EXTRACT_TIMESTAMP_PARAMETER, hecConfig.getAutoExtractTimestamp().toString());
            }
             return  uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
