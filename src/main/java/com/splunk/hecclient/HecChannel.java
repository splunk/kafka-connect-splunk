package com.splunk.hecclient;

import org.apache.http.client.methods.HttpUriRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kchen on 10/18/17.
 */
final class HecChannel {
    private String id;
    private Map<String, String> chField;
    private IndexerInf indexer;

    public HecChannel(IndexerInf idx) {
        id = newChannelId();
        indexer = idx;
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

    public boolean send(final EventBatch batch) {
        if (chField != null) {
            batch.addExtraFields(chField);
        }
        return indexer.send(batch);
    }

    // for convenience
    public String executeHttpRequest(final HttpUriRequest req) {
        return indexer.executeHttpRequest(req);
    }

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
        if (chField != null) {
            chField = null;
        }
        return this;
    }

    private static String newChannelId() {
        return java.util.UUID.randomUUID().toString();
    }
}
