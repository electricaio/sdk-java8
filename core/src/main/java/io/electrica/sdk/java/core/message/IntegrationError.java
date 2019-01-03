package io.electrica.sdk.java.core.message;

import io.electrica.sdk.java.api.exception.IntegrationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * An error that represents a failure of executed integration.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationError {

    private String code;
    private String message;
    private String stackTrace;
    private List<String> payload;

    public IntegrationException asException() {
        return new IntegrationException(code, message, stackTrace, payload);
    }
}
