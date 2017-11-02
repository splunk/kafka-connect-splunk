package com.splunk.hecclient;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpUriRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kchen on 10/31/17.
 */
public final class IndexerMock implements IndexerInf {
    private List<EventBatch> batches = new ArrayList<>();
    private List<HttpUriRequest> requests = new ArrayList<>();
    private String response;

    @Override
    public boolean send(final EventBatch batch) {
        batches.add(batch);
        return true;
    }

    @Override
    public String executeHttpRequest(final HttpUriRequest req) {
        requests.add(req);
        return response;
    }

    @Override
    public String getBaseUrl() {
        return "";
    }

    @Override
    public Header[] getHeaders() {
        return null;
    }

    public List<EventBatch> getBatches() {
        return batches;
    }

    public List<HttpUriRequest> getRequests() {
        return requests;
    }

    public IndexerMock setResponse(String response) {
        this.response = response;
        return this;
    }
}
