package com.splunk.hecclient;

/**
 * Created by kchen on 10/17/17.
 */
public class HecClientException extends RuntimeException {
    private static final long serialVersionUID = 34L;

    public HecClientException(String message) {
        super(message);
    }

    public HecClientException(String message, Throwable cause) {
        super(message, cause);
    }
}