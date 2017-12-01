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

import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;
import java.io.*;

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
