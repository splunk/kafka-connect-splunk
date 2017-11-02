package com.splunk.hecclient;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;

/**
 * Created by kchen on 11/1/17.
 */
public class StatusLineMock implements StatusLine {
    private int status;

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
