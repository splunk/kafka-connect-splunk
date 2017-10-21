package com.splunk.com.splunk.hecclient.examples;

import com.splunk.hecclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by kchen on 10/20/17.
 */
public class HecExample {
    public static void main(String[] args) {
        List<String> uris = Arrays.asList(
                "https://52.53.254.149:8088",
                "https://54.241.138.186:8088",
                "https://54.183.92.156:8088");
        String tokenWithAck = "536AF219-CF36-4C8C-AA0C-FD9793A0F4DD";
        HecClientConfig config = new HecClientConfig(uris, tokenWithAck);
        config.setAckPollerThreads(4)
                .setBatchEventTimeout(60)
                .setDisableSSLCertVerification(true)
                .setHttpKeepAlive(true)
                .setMaxHttpConnectionPoolSizePerIndexer(4);

        Logger log = LoggerFactory.getLogger(IndexerExample.class);

        // Json
        int n = 10;
        int m = 100;

        Thread jsonThr = new Thread(new Runnable() {
            public void run() {
                Hec hec = new HecWithAck(config, new PrintIt());
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
                hec.close();
            }
        });
        jsonThr.start();

        // raw
        Thread rawThr = new Thread(new Runnable() {
            public void run() {
                Hec hec = new HecWithAck(config, new PrintIt());
                for (int j = 0; j < n; j++) {
                    EventBatch batch = new RawEventBatch("main", null,"test-raw-event", null, -1);
                    for (int i = 0; i < m; i++) {
                        Event evt = new RawEvent("my raw message: " +  (m * j + i) + "\n", null);
                        evt.setSourcetype("test-raw-event");
                        batch.add(evt);
                    }
                    log.info("raw batch: " + j);
                    hec.send(batch);
                }
                hec.close();
            }
        });
        rawThr.start();

        try {
            TimeUnit.SECONDS.sleep(6000);
        } catch (InterruptedException ex) {
        }

        log.info("Done");
    }
}
