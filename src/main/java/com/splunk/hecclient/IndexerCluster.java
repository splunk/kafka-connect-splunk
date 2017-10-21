package com.splunk.hecclient;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by kchen on 10/18/17.
 */
public class IndexerCluster {
    private List<Indexer> indexers;
    private int index;

    public IndexerCluster() {
        indexers = new ArrayList<>();
        index = 0;
    }

    public void add(Indexer indexer) {
        indexers.add(indexer);
    }

    public void remove(Indexer indexer) {
        doRemove(indexer.getBaseUrl());
    }

    public void remove(String indexerBaseUrl) {
        doRemove(indexerBaseUrl);
    }

    private void doRemove(String indexerBaseUrl) {
        for (Iterator<Indexer> iter = indexers.listIterator(); iter.hasNext();) {
            Indexer idx = iter.next();
            if (idx.getBaseUrl().equals(indexerBaseUrl)) {
                iter.remove();
            }
        }
    }

    public void send(EventBatch batch) {
        Indexer idx = indexers.get(index);
        index = (index + 1) % indexers.size();
        idx.send(batch);
    }
}