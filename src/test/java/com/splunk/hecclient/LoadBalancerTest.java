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

import com.splunk.hecclient.examples.HecExample;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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
    public void sendWithAllBackPressure() {
        LoadBalancer lb = new LoadBalancer();
        List<IndexerMock> indexers = new ArrayList<>();

        int numberOfChannels = 3;
        for (int i = 0; i < numberOfChannels; i++) {
            IndexerMock indexer = new IndexerMock();
            indexers.add(indexer);
            indexer.setBackPressure(true);
            HecChannel ch = new HecChannel(indexer);
            lb.add(ch);
        }

        lb.send(UnitUtil.createBatch());
    }

    @Test
    public void sendWithOneBackPressure() {
        LoadBalancer lb = new LoadBalancer();
        List<IndexerMock> indexers = new ArrayList<>();

        int numberOfChannels = 3;
        for (int i = 0; i < numberOfChannels; i++) {
            IndexerMock indexer = new IndexerMock();
            indexers.add(indexer);
            HecChannel ch = new HecChannel(indexer);
            lb.add(ch);
        }
        indexers.get(0).setBackPressure(true);

        int numberOfBatches = 12;
        for (int i = 0; i < numberOfBatches; i++) {
            lb.send(UnitUtil.createBatch());
        }

        Assert.assertEquals(0, indexers.get(0).getBatches().size());
        Assert.assertEquals(6, indexers.get(1).getBatches().size());
        Assert.assertEquals(6, indexers.get(2).getBatches().size());
    }

    @Test
    public void sendWithOneNotAvailable() {
        LoadBalancer lb = new LoadBalancer();
        List<IndexerMock> indexers = new ArrayList<>();

        int numberOfChannels = 3;
        for (int i = 0; i < numberOfChannels; i++) {
            IndexerMock indexer = new IndexerMock();
            indexers.add(indexer);
            HecChannel ch = new HecChannel(indexer);
            if(i == 0) {
                ch.setAvailable(false);
            }
            lb.add(ch);
        }

        int numberOfBatches = 12;
        for (int i = 0; i < numberOfBatches; i++) {
            lb.send(UnitUtil.createBatch());
        }

        Assert.assertEquals(0, indexers.get(0).getBatches().size());
        Assert.assertEquals(6, indexers.get(1).getBatches().size());
        Assert.assertEquals(6, indexers.get(2).getBatches().size());
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
