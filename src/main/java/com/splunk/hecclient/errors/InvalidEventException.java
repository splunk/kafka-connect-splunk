package com.splunk.hecclient.errors;

/**
 * Created by kchen on 10/17/17.
 */
public class InvalidEventException extends HecClientException {
    public InvalidEventException(String message) {
        super(message);
    }
}
