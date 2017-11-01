package com.splunk.hecclient;

/**
 * Created by kchen on 10/17/17.
 */
public class HecException extends RuntimeException {
    private static final long serialVersionUID = 34L;

    public HecException(String message) {
        super(message);
    }

    public HecException(String message, Throwable cause) {
        super(message, cause);
    }
}