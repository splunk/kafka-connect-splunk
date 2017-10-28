package com.splunk.hecclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by kchen on 10/27/17.
 */
public class ConcurrentHec {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentHec.class);

    private LinkedBlockingQueue<EventBatch> batches;
    private ExecutorService executorService;
    private List<Hec> hecs;
    private PollerCallback pollerCallback;
    private volatile boolean stopped;

    public ConcurrentHec(int numberOfThreads, boolean useAck, HecClientConfig config, PollerCallback cb) {
        batches = new LinkedBlockingQueue<>(100);
        executorService = Executors.newFixedThreadPool(numberOfThreads);
        initHec(numberOfThreads, useAck, config, cb);
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

    public boolean send(final EventBatch batch) {
        try {
            return batches.offer(batch, 500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            return false;
        }
    }

    public void close() {
        stopped = true;
        // executorService.shutdownNow();
        executorService.shutdown();
    }

    private void run(int id) {
        final Hec hec = hecs.get(id);
        while (!stopped) {
            EventBatch batch;
            try {
                batch = batches.poll(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                continue;
            }

            send(hec, batch);
        }
        hec.close();
    }

    private void send(final Hec hec, final EventBatch batch) {
        batch.resetSendTimestamp();
        try {
            hec.send(batch);
            log.debug("Sent {} events to Splunk", batch.size());
        } catch (Exception ex) {
            batch.fail();
            log.error("sending batch to splunk encountered error", ex);
            pollerCallback.onEventFailure(Arrays.asList(batch), ex);
        }
    }

    private void initHec(int count, boolean useAck, HecClientConfig config, PollerCallback cb) {
        config.setTotalChannels(Math.max(config.getTotalChannels() / count, 1));
        hecs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (useAck) {
                hecs.add(new HecWithAck(config, cb));
            } else {
                hecs.add(new HecWithoutAck(config, cb));
            }
        }
    }
}