package io.electrica.sdk.java8.api.exception;

import java.util.List;
import java.util.StringJoiner;

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

    @Override
    public String toString() {
        return new StringJoiner(", ", IntegrationException.class.getSimpleName() + "[", "]")
                .add("message='" + getMessage() + "'")
                .add("code='" + code + "'")
                .add("stackTrace='" + stackTrace + "'")
                .add("payload=" + payload)
                .toString();
    }
}
