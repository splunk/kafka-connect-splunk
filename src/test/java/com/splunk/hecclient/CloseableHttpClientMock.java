/*
 * Copyright 2017 Splunk, Inc..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.splunk.hecclient;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.*;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

@SuppressWarnings( "deprecation")
public class CloseableHttpClientMock extends CloseableHttpClient {
    public static final String SUCCESS = "{\"text\":\"Success\",\"code\":0,\"ackId\":2}";
    public static final String SERVER_BUSY = "{\"text\":\"Server busy\",\"code\":1}";
    public static final String NO_DATA_ERROR = "{\"text\":\"No data\",\"code\":5}";
    public static final String INVALID_DATA_FORMAT = "{\"text\":\"Invalid data format\",\"code\":6}";
    public static final String INVALID_TOKEN = "{\"text\":\"Invalid token\",\"code\":4}";
    public static final String INVALID_INDEX = "{\"text\":\"Incorrect index\",\"code\":4,\"invalid-event-number\":1}";
    public static final String EXCEPTION = "excpetion";

    private String resp = "";
    private boolean throwOnClose = false;
    private boolean throwOnGetContent = false;

    protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request,
            HttpContext context) throws IOException {
        if (resp == EXCEPTION) {
            throw new IOException("mocked up");
        }

        if (resp.equals(SUCCESS)) {
            return createResponse(resp, 200);
        } else if (resp.equals(SERVER_BUSY)) {
            return createResponse(resp, 503);
        } else if (resp.equals(NO_DATA_ERROR)) {
            return createResponse(resp, 400);
        }else if (resp.equals(INVALID_TOKEN)) {
            return createResponse(resp, 400);
        }else if (resp.equals(INVALID_INDEX)) {
            return createResponse(resp, 400);
        } else {
            return createResponse(SUCCESS, 201);
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
