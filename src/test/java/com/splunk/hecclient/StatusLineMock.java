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

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;

public class StatusLineMock implements StatusLine {
    private final int status;

    public StatusLineMock(int status) {
        this.status = status;
    }

    public ProtocolVersion getProtocolVersion() {
        return new ProtocolVersion("http", 1, 1);
    }

    public int getStatusCode() {
        return status;
    }

    public String getReasonPhrase() {
        return "POST";
    }
}
