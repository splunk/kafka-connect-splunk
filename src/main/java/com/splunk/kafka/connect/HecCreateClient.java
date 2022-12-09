package com.splunk.kafka.connect;

import org.apache.http.impl.client.CloseableHttpClient;

import com.splunk.hecclient.Hec;
import com.splunk.hecclient.HecConfig;

public class HecCreateClient extends HecClosableClient {

    @Override
    CloseableHttpClient getClient(HecConfig config) {
        return Hec.createHttpClient(config);
        
    }

    
}
