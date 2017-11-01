package com.splunk.hecclient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by kchen on 10/19/17.
 */
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

    public PostResponse setCode(int c) {
        code = c;
        return this;
    }

    public PostResponse setText(String t) {
        text = t;
        return this;
    }

    public PostResponse setAckId(long id) {
        ackId = id;
        return this;
    }
}