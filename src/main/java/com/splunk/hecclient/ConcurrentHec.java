package com.splunk.hecclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by kchen on 10/27/17.
 */
public class ConcurrentHec implements HecInf {
    private static final Logger log = LoggerFactory.getLogger(ConcurrentHec.class);

    private LinkedBlockingQueue<EventBatch> batches;
    private ExecutorService executorService;
    private List<Hec> hecs;
    private PollerCallback pollerCallback;
    private volatile boolean stopped;

    public ConcurrentHec(int numberOfThreads, boolean useAck, HecConfig config, PollerCallback cb) {
        this(numberOfThreads, useAck, config, cb, new LoadBalancer());
    }

    public ConcurrentHec(int numberOfThreads, boolean useAck, HecConfig config, PollerCallback cb, LoadBalancerInf loadBalancer) {
        batches = new LinkedBlockingQueue<>(100);
        ThreadFactory e = (Runnable r) -> new Thread(r, "Concurrent-HEC-worker");
        executorService = Executors.newFixedThreadPool(numberOfThreads, e);
        initHec(numberOfThreads, useAck, config, cb, loadBalancer);
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
            batches.offer(batch, 1000, TimeUnit.MILLISECONDS);
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
        // executorService.shutdownNow();
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
