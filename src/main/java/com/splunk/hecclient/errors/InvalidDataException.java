package com.splunk.hecclient.errors;

/**
 * Created by kchen on 10/19/17.
 */
public class InvalidDataException extends HecClientException {
    public InvalidDataException(String message, Throwable cause) {
        super(message, cause);
    }
}