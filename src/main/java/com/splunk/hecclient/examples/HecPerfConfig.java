package com.splunk.hecclient.examples;

import com.splunk.hecclient.HecConfig;

/**
 * Created by kchen on 10/20/17.
 */
public final class HecPerfConfig {
    private HecConfig config;
    private int concurrency;
    private int clientPoolSize;
    private int iterations;

    public HecPerfConfig(HecConfig config, int concurrency, int clientPoolSize, int iterations) {
        this.config = config;
        this.concurrency = concurrency;
        this.clientPoolSize = clientPoolSize;
        this.iterations = iterations;
    }

    public HecConfig getHecClientConfig() {
        return config;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public int getClientPoolSize() {
        return clientPoolSize;
    }

    public int getIterations() {
        return iterations;
    }
}
