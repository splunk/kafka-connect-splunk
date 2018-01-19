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

import org.apache.http.Header;
import org.apache.http.client.methods.HttpUriRequest;

import java.util.ArrayList;
import java.util.List;

public final class IndexerMock implements IndexerInf {
    private List<EventBatch> batches = new ArrayList<>();
    private List<HttpUriRequest> requests = new ArrayList<>();
    private String response;
    private boolean backPressure = false;

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

    @Override
    public boolean hasBackPressure() {
        return backPressure;
    }

    public IndexerMock setBackPressure(boolean backPressure) {
        this.backPressure = backPressure;
        return this;
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
