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

import org.apache.http.client.methods.HttpUriRequest;

import java.util.HashMap;
import java.util.Map;

final class HecChannel {
    private String id;
    private Map<String, String> chField;
    private IndexerInf indexer;
    private boolean isAvailable;

    public HecChannel(IndexerInf idx) {
        id = newChannelId();
        indexer = idx;
        isAvailable = true;
    }

    public IndexerInf getIndexer() {
        return indexer;
    }

    public String getId() {
        return id;
    }

    public HecChannel setTracking(boolean trackChannel) {
        if (trackChannel) {
            enableTracking();
        } else {
            disableTracking();
        }

        return this;
    }

    public void setId() { id = newChannelId(); }

    public void setAvailable(boolean isAvailable) { this.isAvailable = isAvailable; }

    public void send(final EventBatch batch) {
        if (chField != null) {
            batch.addExtraFields(chField);
        }
        indexer.send(batch);
    }

    // for convenience
    public String executeHttpRequest(final HttpUriRequest req) {
        return indexer.executeHttpRequest(req);
    }

    public boolean hasBackPressure() { return indexer.hasBackPressure(); }

    public boolean isNotAvailable() { return isAvailable == false; }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof HecChannel) {
            HecChannel ch = (HecChannel) obj;
            return id.equals(ch.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }

    private HecChannel enableTracking() {
        if (chField == null) {
            chField = new HashMap<>();
            chField.put("hec-channel", id);
        }
        return this;
    }

    private HecChannel disableTracking() {
        chField = null;
        return this;
    }

    private static String newChannelId() {
        return java.util.UUID.randomUUID().toString();
    }
}
