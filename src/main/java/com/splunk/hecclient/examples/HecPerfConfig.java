package com.splunk.hecclient.examples;

import com.splunk.hecclient.HecClientConfig;

/**
 * Created by kchen on 10/20/17.
 */
public class HecPerfConfig {
    private HecClientConfig config;
    private int concurrency;
    private int clientPoolSize;
    private int iterations;

    public HecPerfConfig(HecClientConfig config, int concurrency, int clientPoolSize, int iterations) {
        this.config = config;
        this.concurrency = concurrency;
        this.clientPoolSize = clientPoolSize;
        this.iterations = iterations;
    }

    public HecClientConfig getHecClientConfig() {
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
