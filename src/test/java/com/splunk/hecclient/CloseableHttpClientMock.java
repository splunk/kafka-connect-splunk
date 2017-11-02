package com.splunk.hecclient;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.*;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Created by kchen on 11/1/17.
 */
@SuppressWarnings( "deprecation")
public class CloseableHttpClientMock extends CloseableHttpClient {
    public static final String success = "{\"text\":\"Success\",\"code\":0,\"ackId\":2}";
    public static final String serverBusy = "{\"text\":\"Server busy\",\"code\":1}";
    public static final String noDataError = "{\"text\":\"No data\",\"code\":5}";
    public static final String exception = "excpetion";

    private String resp = "";
    private boolean throwOnClose = false;
    private boolean throwOnGetContent = false;

    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request,
            HttpContext context) throws IOException {
        if (resp == exception) {
            throw new IOException("mocked up");
        }

        if (resp.equals(success)) {
            return createResponse(resp, 200);
        } else if (resp.equals(serverBusy)) {
            return createResponse(resp, 503);
        } else if (resp.equals(noDataError)) {
            return createResponse(resp, 400);
        } else {
            return createResponse(success, 201);
        }
    }

    private CloseableHttpResponse createResponse(String content, int statusCode) {
        HttpEntityMock entity = new HttpEntityMock();
        entity.setThrowOnGetContent(throwOnGetContent);
        entity.setContent(content);

        StatusLineMock status = new StatusLineMock(statusCode);

        CloseableHttpResponseMock resp = new CloseableHttpResponseMock();
        resp.setThrowOnClose(throwOnClose);
        resp.setEntity(entity);
        resp.setStatusLine(status);
        return resp;
    }

    public CloseableHttpClientMock setResponse(final String resp) {
        this.resp = resp;
        return this;
    }

    public CloseableHttpClientMock setThrowOnClose(final boolean th) {
        this.throwOnClose = th;
        return this;
    }

    public CloseableHttpClientMock setThrowOnGetContent(final boolean th) {
        this.throwOnGetContent = th;
        return this;
    }


    @Override
    @Deprecated
    public ClientConnectionManager getConnectionManager() {
        return null;
    }

    @Override
    @Deprecated
    public HttpParams getParams() {
        return null;
    }

    @Override
    public void close() throws IOException {
    }
}
