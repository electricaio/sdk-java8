package io.electrica.sdk.java.core.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class AckOutboundMessage {

    private final UUID correlationId;
    private final Boolean accepted;

}
