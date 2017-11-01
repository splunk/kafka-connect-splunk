package com.splunk.hecclient;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kchen on 10/31/17.
 */
public class LoadBalancerTest {
    @Test
    public void add() {
        LoadBalancer lb = new LoadBalancer();

        int numberOfChannels = 3;
        for (int i = 0; i < numberOfChannels; i++) {
            IndexerMock indexer = new IndexerMock();
            HecChannel ch = new HecChannel(indexer);
            lb.add(ch);
        }

        Assert.assertEquals(numberOfChannels, lb.size());
    }

    @Test
    public void send() {
        LoadBalancer lb = new LoadBalancer();
        List<IndexerMock> indexers = new ArrayList<>();

        int numberOfChannels = 3;
        for (int i = 0; i < numberOfChannels; i++) {
            IndexerMock indexer = new IndexerMock();
            indexers.add(indexer);
            HecChannel ch = new HecChannel(indexer);
            lb.add(ch);
        }

        int numberOfBatches = 12;
        for (int i = 0; i < numberOfBatches; i++) {
            lb.send(UnitUtil.createBatch());
        }

        // the requests should be divided evenly across the indexer
        for (IndexerMock indexer: indexers) {
            Assert.assertEquals(numberOfBatches / numberOfChannels, indexer.getBatches().size());
        }
    }

    @Test(expected = HecException.class)
    public void sendWithoutChannels() {
        LoadBalancer lb = new LoadBalancer();
        lb.send(UnitUtil.createBatch());
    }

    @Test
    public void remove() {
        LoadBalancer lb = new LoadBalancer();
        List<HecChannel> channels = new ArrayList<>();

        int numberOfChannels = 3;
        for (int i = 0; i < numberOfChannels; i++) {
            IndexerMock indexer = new IndexerMock();
            HecChannel ch = new HecChannel(indexer);
            channels.add(ch);
            lb.add(ch);
        }

        for (HecChannel ch: channels) {
            lb.remove(ch);
        }

        Assert.assertEquals(0, lb.size());
    }

    @Test
    public void size() {
        LoadBalancer lb = new LoadBalancer();
        Assert.assertEquals(0, lb.size());
    }
}
