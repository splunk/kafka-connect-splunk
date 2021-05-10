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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
final class PostResponse {
    // {"text":"Success","code":0,"ackId":7}
    private String text;
    private int code = -1;
    private long ackId = -1;

    PostResponse() {
    }

    public boolean isSucceed() {
        return code == 0;
    }

    public String getText() {
        return text;
    }

    public long getAckId() {
        return ackId;
    }

    public void setCode(int c) {
        code = c;
    }

    public void setText(String t) {
        text = t;
    }

    public void setAckId(long id) {
        ackId = id;
    }
}
