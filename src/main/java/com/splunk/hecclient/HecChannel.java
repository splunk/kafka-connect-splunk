package com.splunk.hecclient;

/**
 * Created by kchen on 10/18/17.
 */
public class HecChannel {
    private String id;
    private Indexer indexer;

    public HecChannel(Indexer idx) {
        id = newChannelId();
        indexer = idx;
    }

    public Indexer getIndexer() {
        return indexer;
    }

    public String getId() {
        return id;
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
    public String toString() {
        return id;
    }

    private static String newChannelId() {
        return java.util.UUID.randomUUID().toString();
    }
}