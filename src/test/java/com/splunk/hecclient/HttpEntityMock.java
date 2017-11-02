package com.splunk.hecclient;

import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;
import java.io.*;

/**
 * Created by kchen on 11/1/17.
 */
public class HttpEntityMock extends AbstractHttpEntity {
    private String content = "";
    private boolean throwOnGet = false;

    public HttpEntityMock setContent(final String content) {
        this.content = content;
        return this;
    }

    public HttpEntityMock setThrowOnGetContent(boolean th) {
        throwOnGet = th;
        return this;
    }

    @Override
    public boolean isRepeatable() {
                                return true;
                                            }

    @Override
    public long getContentLength() {
        return content.length();
    }

    @Override
    public boolean isStreaming() {
                               return false;
                                            }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        if (throwOnGet) {
            throw new IOException("mocked up");
        }
        return new ByteArrayInputStream(content.getBytes());
    }

    @Override
    public void writeTo(OutputStream outstream) throws IOException {
        outstream.write(content.getBytes());
    }
}
