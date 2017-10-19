package com.splunk.hecclient.errors;

/**
 * Created by kchen on 10/18/17.
 */
public class InvalidHttpProtocolException extends HecClientException {
    public InvalidHttpProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
