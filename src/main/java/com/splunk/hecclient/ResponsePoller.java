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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public final class ResponsePoller implements Poller {
    private static final Logger log = LoggerFactory.getLogger(ResponsePoller.class);
    private static final ObjectMapper jsonMapper = new ObjectMapper();

    private PollerCallback callback;

    public ResponsePoller(PollerCallback callback) {
        this.callback = callback;
    }

    @Override
    public void stickySessionHandler(HecChannel channel) {}

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void fail(HecChannel channel, EventBatch batch, Exception ex) {
        batch.fail();
        if (callback != null) {
            callback.onEventFailure(Arrays.asList(batch), ex);
        }
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
        try {
            PostResponse response = jsonMapper.readValue(resp, PostResponse.class);
            if (!response.isSucceed()) {
                fail(channel, batch, new HecException(response.getText()));
                return;
            }
            if (response.getText() == "Invalid data format") {
                log.warn("Invalid Splunk HEC data format. Ignoring events. channel={} index={} events={}", channel, channel.getIndexer(), batch.toString());
            }
        } catch (Exception ex) {
            log.error("failed to parse response", resp, ex);
            fail(channel, batch, ex);
            return;
        }

        batch.commit();
        if (callback != null) {
            callback.onEventCommitted(Arrays.asList(batch));
        }
    }

    public void setStickySessionToTrue() {}
}
