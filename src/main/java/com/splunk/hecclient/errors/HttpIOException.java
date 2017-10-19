package com.splunk.hecclient.errors;

/**
 * Created by kchen on 10/18/17.
 */
public class HttpIOException extends HecClientException {
    public HttpIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
