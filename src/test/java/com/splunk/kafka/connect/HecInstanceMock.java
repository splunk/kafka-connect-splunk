package com.splunk.kafka.connect;

import org.apache.http.impl.client.CloseableHttpClient;

import com.splunk.hecclient.CloseableHttpClientMock;
import com.splunk.hecclient.Hec;
import com.splunk.hecclient.HecConfig;

public class HecInstanceMock extends HecClosableClient{

    @Override
    CloseableHttpClient getClient(HecConfig config) {
        // TODO Auto-generated method stub
        if (config==null){}
        CloseableHttpClientMock client = new CloseableHttpClientMock();
        return client;
    }
    
}
