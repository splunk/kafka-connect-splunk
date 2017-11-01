package com.splunk.hecclient;

import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by kchen on 10/31/17.
 */

public class StreamReader {
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
                throw new HecClientException("failed to read from stream", ex);
            }
        }
        return siz;
    }
}
