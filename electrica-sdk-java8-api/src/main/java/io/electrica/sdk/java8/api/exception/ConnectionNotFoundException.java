package io.electrica.sdk.java8.api.exception;

/**
 * Represents an exception that is thrown if a connection does not exist for given ern and access key.
 */
public class ConnectionNotFoundException extends RuntimeException {

    public ConnectionNotFoundException(String message) {
        super(message);
    }

    public ConnectionNotFoundException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
