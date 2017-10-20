package com.splunk.hecclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Created by kchen on 10/18/17.
 */
public class ResponsePoller implements Poller {
    private final static Logger log = LoggerFactory.getLogger(ResponsePoller.class);
    private final static ObjectMapper jsonMapper = new ObjectMapper();

    private PollerCallback callback;

    public ResponsePoller(PollerCallback callback) {
        this.callback = callback;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void fail(HecChannel channel, EventBatch batch) {
        if (callback != null) {
            callback.onEventFailure(Arrays.asList(batch));
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
                fail(channel, batch);
            }
        } catch (Exception ex) {
            log.error("failed to parse response", resp, ex);
            fail(channel, batch);
            return;
        }

        if (callback != null) {
            callback.onEventCommitted(Arrays.asList(batch));
        }
    }
}