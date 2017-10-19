package com.splunk.hecclient.errors;

/**
 * Created by kchen on 10/19/17.
 */
public class ShutdownException extends HecClientException {
    public ShutdownException(String message, Throwable cause) {
        super(message, cause);
    }
}