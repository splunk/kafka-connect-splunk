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

import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.HeaderGroup;
import org.apache.http.params.*;

import java.io.IOException;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class CloseableHttpResponseMock implements CloseableHttpResponse {
    private StatusLine statusLine;
    private HttpEntity entity;
    private boolean throwOnClose = false;
    private HeaderGroup headers = new HeaderGroup();

    public CloseableHttpResponseMock setThrowOnClose(boolean th) {
        throwOnClose = th;
        return this;
    }
    @Override
    public void close() throws IOException {
        if (throwOnClose) {
            throw new IOException("mockup");
        }
    }

    @Override
    public StatusLine getStatusLine() {
        return statusLine;
    }

    @Override
    public void setStatusLine(final StatusLine statusline) {
        this.statusLine = statusline;
    }

    @Override
    public void setStatusLine(final ProtocolVersion ver, final int code) {
    }

    @Override
    public void setStatusLine(final ProtocolVersion ver, final int code, final String reason) {
    }

    @Override
    public void setStatusCode(final int code) throws IllegalStateException {
    }

    @Override
    public void setReasonPhrase(final String reason) throws IllegalStateException {
    }

    @Override
    public HttpEntity getEntity() {
        return entity;
    }

    @Override
    public void setEntity(final HttpEntity entity) {
        this.entity = entity;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public void setLocale(final Locale loc) {
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return null;
    }

    @Override
    public boolean containsHeader(final String name) {
        return headers.containsHeader(name);
    }

    @Override
    public Header[] getHeaders(final String name) {
        return headers.getHeaders(name);
    }

    @Override
    public Header getFirstHeader(final String name) {
        return headers.getFirstHeader(name);
    }

    @Override
    public Header getLastHeader(final String name) {
        return headers.getLastHeader(name);
    }

    @Override
    public Header[] getAllHeaders() {
        return headers.getAllHeaders();
    }

    @Override
    public void addHeader(final Header header) {
        headers.addHeader(header);
    }

    @Override
    public void addHeader(final String name, final String value) {
        headers.addHeader(new org.apache.http.message.BasicHeader(name, value));
    }

    @Override
    public void setHeader(final Header header) {
        headers.updateHeader(header);
    }

    @Override
    public void setHeader(final String name, final String value) {
        headers.updateHeader(new org.apache.http.message.BasicHeader(name, value));
    }

    @Override
    public void setHeaders(final Header[] headers) {
        this.headers.setHeaders(headers);
    }

    @Override
    public void removeHeader(final Header header) {
        headers.removeHeader(header);
    }

    @Override
    public void removeHeaders(final String name) {
        HeaderIterator iterator = headers.iterator(name);
        while (iterator.hasNext()) {
            iterator.nextHeader();
            iterator.remove();
        }
    }

    @Override
    public HeaderIterator headerIterator() {
        return headers.iterator();
    }

    @Override
    public HeaderIterator headerIterator(final String name) {
        return headers.iterator(name);
    }

    @Override
    @Deprecated
    public HttpParams getParams() {
        return null;
    }

    @Override
    @Deprecated
    public void setParams(final HttpParams params) {
    }
}
