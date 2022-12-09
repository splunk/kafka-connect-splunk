package com.splunk.kafka.connect;

import org.apache.http.impl.client.CloseableHttpClient;

import com.splunk.hecclient.CloseableHttpClientMock;
import com.splunk.hecclient.Hec;
import com.splunk.hecclient.HecConfig;

public class MockHecClientWrapper extends AbstractClientWrapper{
    public CloseableHttpClientMock client = new CloseableHttpClientMock();

    @Override
    CloseableHttpClient getClient(HecConfig config) {
        // TODO Auto-generated method stub
        if (config==null){}
        
        return client;
    }
    
}
