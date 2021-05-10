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
package com.splunk.hecclient.examples;

import com.splunk.hecclient.HecConfig;

public final class HecPerfConfig {
    private final HecConfig config;
    private final int concurrency;
    private final int clientPoolSize;
    private final int iterations;

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
