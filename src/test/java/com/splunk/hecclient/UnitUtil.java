package com.splunk.hecclient;

import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Created by kchen on 11/1/17.
 */
public class UnitUtil {
    public static HecConfig createHecConfig() {
        return new HecConfig(Arrays.asList("https://dummyhost:8088"), "token");
    }

    public static EventBatch createBatch() {
        EventBatch batch = new JsonEventBatch();
        Event event = new JsonEvent("ni", "hao");
        batch.add(event);
        return batch;
    }

    public static void milliSleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException ex) {
        }
    }

    public static int read(final InputStream stream, byte[] data) {
        int siz = 0;
        while (true) {
            try {
                int read = stream.read(data, siz, data.length - siz);
                if (read < 0) {
                    break;
                }
                siz += read;
            } catch (IOException ex) {
                Assert.assertTrue("failed to read from stream", false);
                throw new HecException("failed to read from stream", ex);
            }
        }
        return siz;
    }
}
