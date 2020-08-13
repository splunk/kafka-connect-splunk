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

import com.splunk.hecclient.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class HecExample {
    public static void main(String[] args) {
        Logger log = LoggerFactory.getLogger(HecExample.class);

        List<String> uris = Arrays.asList("https://localhost:8088");
        String tokenWithAck = "936AF219-CF36-4C8C-AA0C-FD9793A0F4D4";
        HecConfig config = new HecConfig(uris, tokenWithAck);
        config.setAckPollInterval(10)
                .setEventBatchTimeout(60)
                .setDisableSSLCertVerification(true)
                .setHttpKeepAlive(true)
                .setMaxHttpConnectionPerChannel(4);

        CloseableHttpClient httpClient = Hec.createHttpClient(config);
        Poller poller = Hec.createPoller(config, new PrintIt());

        // Json
        int n = 100000;
        int m = 100;

        Hec hec = new Hec(config, httpClient, poller, new LoadBalancer(config, httpClient));
        Thread jsonThr = new Thread(new Runnable() {
            public void run() {
                for (int j = 0; j < n; j++) {
                    EventBatch batch = new JsonEventBatch();
                    for (int i = 0; i < m; i++) {
                        Event evt = new JsonEvent("my message: " +  (m * j + i), null);
                        evt.setSourcetype("test-json-event");
                        batch.add(evt);
                    }
                    log.info("json batch: " + j);
                    hec.send(batch);
                }
            }
        });
        jsonThr.start();

        // raw
        Hec rawHec = new Hec(config, httpClient, poller, new LoadBalancer(config, httpClient));
        Thread rawThr = new Thread(new Runnable() {
            public void run() {
                for (int j = 0; j < n; j++) {
                    EventBatch batch = new RawEventBatch("main", null,"test-raw-event", null, -1);
                    for (int i = 0; i < m; i++) {
                        Event evt = new RawEvent("my raw message: " +  (m * j + i) + "\n", null);
                        evt.setSourcetype("test-raw-event");
                        batch.add(evt);
                    }
                    log.info("raw batch: " + j);
                    rawHec.send(batch);
                }
            }
        });
        rawThr.start();

        try {
            TimeUnit.SECONDS.sleep(6000);
        } catch (InterruptedException ex) {
        }

        hec.close();
        rawHec.close();
        log.info("Done");
    }
}
