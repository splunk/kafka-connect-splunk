package com.splunk.hecclient;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpUriRequest;

/**
 * Created by kchen on 10/31/17.
 */
interface IndexerInf {
    boolean send(final EventBatch batch);
    String executeHttpRequest(final HttpUriRequest req);
    boolean hasBackPressure();
    String getBaseUrl();
    Header[] getHeaders();
}
