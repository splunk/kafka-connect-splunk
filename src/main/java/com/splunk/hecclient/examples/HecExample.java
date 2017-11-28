package com.splunk.hecclient.examples;

import com.splunk.hecclient.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by kchen on 10/20/17.
 */
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

        CloseableHttpClient httpCilent = Hec.createHttpClient(config);
        Poller poller = Hec.createPoller(config, new PrintIt());

        // Json
        int n = 100000;
        int m = 100;

        Hec hec = new Hec(config, httpCilent, poller, new LoadBalancer());
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
        Hec rawHec = new Hec(config, httpCilent, poller, new LoadBalancer());
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
