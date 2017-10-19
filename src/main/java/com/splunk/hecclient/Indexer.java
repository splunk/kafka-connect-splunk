package com.splunk.hecclient;

import com.splunk.hecclient.errors.HttpIOException;
import com.splunk.hecclient.errors.InvalidHttpProtocolException;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by kchen on 10/18/17.
 */
public class Indexer {
    private static final Logger log = LoggerFactory.getLogger(Indexer.class);
    private CloseableHttpClient httpClient;
    private String baseUrl;
    private String hecToken;
    private HecChannel channel;
    private Header[] headers;
    private AckPoller poller;

    // Indexer doesn't own client, ack poller
    public Indexer(String baseUrl, String hecToken, CloseableHttpClient client, AckPoller poller) {
        this.httpClient = client;
        this.baseUrl = baseUrl;
        this.hecToken = hecToken;
        this.poller = poller;

        channel = new HecChannel(this);

        // init headers
        headers = new Header[2];
        headers[0] = new BasicHeader("Authorization", String.format("Splunk %s", hecToken));
        headers[1] = new BasicHeader("X-Splunk-Request-Channel", channel.getId());
    }

    public String getToken() {
        return hecToken;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void send(EventBatch batch) {
        String endpoint = batch.getRestEndpoint();
        String url = baseUrl + endpoint;
        final HttpPost httpPost = new HttpPost(url);
        httpPost.setHeaders(headers);
        httpPost.setEntity(batch.getHttpEntity());

        try {
            CloseableHttpResponse resp = httpClient.execute(httpPost);
        } catch (ClientProtocolException ex) {
            log.error("encountered http protocol exception:", ex);
            throw new InvalidHttpProtocolException("encountered protocol exception", ex);
        } catch (IOException ex) {
            log.error("encountered io exception:", ex);
            throw new HttpIOException("encountered io exception when post data", ex);
        }

        poller.add(channel, batch);
    }
}