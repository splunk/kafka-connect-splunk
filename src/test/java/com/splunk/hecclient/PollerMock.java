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

public class PollerMock implements Poller {
    private boolean started;
    private HecChannel channel;
    private EventBatch batch;
    private EventBatch failedBatch;
    private String response;
    private Exception exception;

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public void fail(HecChannel channel, EventBatch batch, Exception ex) {
        this.channel = channel;
        this.failedBatch = batch;
        this.exception = ex;
    }

    @Override
    public long getTotalOutstandingEventBatches() {
        return 0;
    }

    @Override
    public HecChannel getMinLoadChannel() {
        return null;
    }

    @Override
    public void add(HecChannel channel, EventBatch batch, String resp) {
        this.channel = channel;
        this.batch = batch;
        this.response = resp;
    }

    @Override
    public void stickySessionHandler(HecChannel channel) {}

    public boolean isStarted() {
        return started;
    }

    public HecChannel getChannel() {
        return channel;
    }

    public EventBatch getBatch() {
        return batch;
    }

    public EventBatch getFailedBatch() {
        return failedBatch;
    }

    public Exception getException() {
        return exception;
    }

    public String getResponse() {
        return response;
    }
    public void setStickySessionToTrue() {
    }
}
