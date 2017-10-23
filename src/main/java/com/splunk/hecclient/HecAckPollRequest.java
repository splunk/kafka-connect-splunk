package com.splunk.hecclient;

import org.apache.http.client.methods.HttpUriRequest;

/**
 * Created by kchen on 10/22/17.
 */
public final class HecAckPollRequest {
    private HttpUriRequest request;
    private HecAckPoller ackPoller;
    private HecChannel channel;
    private long expiration;

    public HecAckPollRequest(HttpUriRequest req, HecAckPoller poller, HecChannel ch, int ttl) {
        request = req;
        ackPoller = poller;
        channel = ch;
        expiration = System.currentTimeMillis() / 1000 + ttl;

    }

    public HttpUriRequest getRequest() {
        return request;
    }

    public void handleAckPollResponse(String resp) {
        ackPoller.handleAckPollResponse(resp, channel);
    }

    public boolean isTimedout() {
        return System.currentTimeMillis() / 1000 >= expiration;
    }
}