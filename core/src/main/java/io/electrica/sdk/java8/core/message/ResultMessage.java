package io.electrica.sdk.java8.core.message;

import com.google.gson.JsonElement;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * General result from the invocation service.
 */
@Getter
@Setter
public class ResultMessage {

    private UUID invocationId;
    private UUID instanceId;
    private Long connectionId;
    private JsonElement result;
    private Boolean success;
    private IntegrationError error;

}
