package com.splunk.hecclient.errors;

/**
 * Created by kchen on 10/18/17.
 */
public class InvalidEventTypeException extends HecClientException {
    public InvalidEventTypeException(String message) {
        super(message);
    }
}
