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

import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class UnitUtil {
    public static HecConfig createHecConfig() {
         return new HecConfig(Arrays.asList("https://dummyhost:8088"), "token")
            .setKerberosPrincipal("");
    }

    public static EventBatch createBatch() {
        EventBatch batch = new JsonEventBatch();
        Event event = new JsonEvent("ni", "hao");
        batch.add(event);
        return batch;
    }

    public static EventBatch createRawEventBatch() {
        Event event = new RawEvent("ni", "hao");
        EventBatch batch = RawEventBatch.factory().build();
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
