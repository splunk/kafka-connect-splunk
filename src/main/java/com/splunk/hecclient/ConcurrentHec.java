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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class ConcurrentHec implements HecInf {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentHec.class);

    private LinkedBlockingQueue<EventBatch> batches;
    private ExecutorService executorService;
    private List<Hec> hecs;
    private PollerCallback pollerCallback;
    private volatile boolean stopped;

    public ConcurrentHec(int numberOfThreads, boolean useAck, HecConfig config, PollerCallback cb) {
        this(numberOfThreads, useAck, config, cb, new LoadBalancer(config, null));
    }

    public ConcurrentHec(int numberOfThreads, boolean useAck, HecConfig config, PollerCallback cb, LoadBalancerInf loadBalancer) {
        batches = new LinkedBlockingQueue<>(config.getConcurrentHecQueueCapacity());
        ThreadFactory e = (Runnable r) -> new Thread(r, "Concurrent-HEC-worker");
        executorService = Executors.newFixedThreadPool(numberOfThreads, e);
        initHec(numberOfThreads, useAck, config, cb, loadBalancer);
        loadBalancer.setHttpClient(hecs.get(0).getHttpClient());
        pollerCallback = cb;
        stopped = false;

        for (int i = 0; i < numberOfThreads; i++) {
            final int id = i;
            Runnable r = () -> {
                run(id);
            };
            executorService.submit(r);
        }
    }

    @Override
    public final void send(final EventBatch batch) {
        try {
            boolean offerSuccess = batches.offer(batch, 1000, TimeUnit.MILLISECONDS);
            if (!offerSuccess) {
                log.warn("Linked blocking queue is full (size = {}) for event batch = {}, failed to offer batch into queue", batches.size(), batch.getUUID());
                throw new HecException("linked blocking event queue is full, failed to offer batch into queue");
            }
        } catch (InterruptedException ex) {
            throw new HecException("failed to offer batch into queue", ex);
        }
    }

    @Override
    public final void close() {
        if (stopped) {
            return;
        }

        stopped = true;
        executorService.shutdown();
    }

    private void run(int id) {
        // Note, never exit this function unless a shutdown, otherwise the worker thread will be gone.
        final Hec hec = hecs.get(id);
        while (!stopped) {
            EventBatch batch;
            try {
                batch = batches.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                continue;
            }

            if (batch != null) {
                send(hec, batch);
            }
        }
        hec.close();
    }

    private void send(final Hec hec, final EventBatch batch) {
        try {
            hec.send(batch);
        } catch (Exception ex) {
            batch.fail();
            pollerCallback.onEventFailure(Arrays.asList(batch), ex);
            log.error("sending batch to splunk encountered error", ex);
        }
    }

    private void initHec(int count, boolean useAck, HecConfig config, PollerCallback cb, LoadBalancerInf loadBalancer) {
        config.setTotalChannels(Math.max(config.getTotalChannels() / count, 1));
        hecs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (useAck) {
                hecs.add(Hec.newHecWithAck(config, cb, loadBalancer));
            } else {
                hecs.add(Hec.newHecWithoutAck(config, cb, loadBalancer));
            }
        }
    }
}
