package io.electrica.sdk.java8.api.exception;

import java.util.List;

public class IntegrationException extends Exception {

    private final String code;
    private final String stackTrace;
    private final List<String> payload;

    public IntegrationException(String code, String message, String stackTrace, List<String> payload) {
        super(message);
        this.code = code;
        this.stackTrace = stackTrace;
        this.payload = payload;
    }

    public String getCode() {
        return code;
    }

    public String getStackTraceString() {
        return stackTrace;
    }

    public List<String> getPayload() {
        return payload;
    }
}
