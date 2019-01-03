package io.electrica.sdk.java8.api.http;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class Request {

    private final UUID instanceId;
    private final Long connectionId;
    private final String action;
    @Nullable
    private final Object parameters;
    @Nullable
    private final Object payload;

}
