package com.splunk.hecclient;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by kchen on 10/18/17.
 */
// Indexer is not multi-thread safe
public class Indexer {
    private static final Logger log = LoggerFactory.getLogger(Indexer.class);

    private CloseableHttpClient httpClient;
    private HttpContext context;
    private String baseUrl;
    private String hecToken;
    private boolean keepAlive;
    private HecChannel channel;
    private Header[] headers;
    private Poller poller;

    private ConcurrentLinkedQueue<HecAckPollRequest> ackRequests;

    // Indexer doesn't own client, ack poller
    public Indexer(String baseUrl, String hecToken, CloseableHttpClient client, Poller poller) {
        this.httpClient = client;
        this.baseUrl = baseUrl;
        this.hecToken = hecToken;
        this.poller = poller;
        this.context = HttpClientContext.create();

        channel = new HecChannel(this);
        ackRequests = new ConcurrentLinkedQueue<>();

        // Init headers
        headers = new Header[3];
        headers[0] = new BasicHeader("Authorization", String.format("Splunk %s", hecToken));
        headers[1] = new BasicHeader("X-Splunk-Request-Channel", channel.getId());

        keepAlive = false;
        setKeepAlive(true);
    }

    public Indexer setKeepAlive(boolean keepAlive) {
        if (this.keepAlive == keepAlive) {
            return this;
        }

        if (keepAlive) {
            headers[2] = new BasicHeader("Connection", "Keep-Alive");
        } else {
            headers[2] = new BasicHeader("Connection", "close");
        }
        this.keepAlive = keepAlive;
        return this;
    }

    public boolean getKeepAlive(boolean keepAlive) {
        return keepAlive;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public String getToken() {
        return hecToken;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public HecChannel getChannel() {
        return channel;
    }

    public void send(EventBatch batch) {
        String endpoint = batch.getRestEndpoint();
        String url = baseUrl + endpoint;
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeaders(headers);
        httpPost.setEntity(batch.getHttpEntity());

        String resp;
        try {
            resp = doSend(httpPost);
        } catch (HecClientException ex) {
            poller.fail(channel, batch);
            return;
        }

        // we are all good
        poller.add(channel, batch, resp);
    }

    private String doSend(final HttpUriRequest req) {
        CloseableHttpResponse resp = null;
        try {
            resp = httpClient.execute(req, context);
        } catch (Exception ex) {
            log.error("encountered io exception:", ex);
            throw new HecClientException("encountered exception when post data", ex);
        }

        // read the response payload
        String respPayload;
        HttpEntity entity = resp.getEntity();
        try {
            respPayload = EntityUtils.toString(entity, "utf-8");
        } catch (Exception ex) {
            log.error("failed to process http response", ex);
            throw new HecClientException("failed to process http response", ex);
        } finally {
            try {
                resp.close();
            } catch (Exception ex) {
                throw new HecClientException("failed to close http response", ex);
            }
        }

        // log.info("event posting, channel={}, cookies={}", channel, resp.getHeaders("Set-Cookie"));
        int status = resp.getStatusLine().getStatusCode();
        if (status != 200 && status != 201) {
            log.error("failed to post events resp={}, status={}", respPayload, status);
            throw new HecClientException(String.format("failed to post events resp=%s, status=%d", respPayload, status));
        }

        return respPayload;
    }

    // multi-thread safe. Just add ack poll request to queue
    // next time the thread which owns this indexer will loop the ack request queue
    // and do HTTP stuff. Here we are trying to do HEC event POST and HEC ACK polling
    // for the POSTed events in the same thread since HttpContext is not thread safe.
    // We need use the same HttpContext for both HEC request since we will need retain
    // sticky session if there is any for load balancer case
    public void handleAckPollRequest(final HecAckPollRequest req) {
        ackRequests.add(req);
    }

    // sendAckPollRequests shall be scheduled / called in the same thread as `send`
    // to make sure the integrity of the `http context`
    public void sendAckPollRequests() {
        log.info("handling {} ack poll requests", ackRequests.size());
        while (!ackRequests.isEmpty()) {
            HecAckPollRequest req = ackRequests.poll();
            if (req.isTimedout()) {
                continue;
            }

            String resp;
            try {
                resp = doSend(req.getRequest());
            } catch (HecClientException ex) {
                continue;
            }

            req.handleAckPollResponse(resp);
        }
    }

    @Override
    public String toString() {
        return baseUrl + "@" + channel;
    }
}