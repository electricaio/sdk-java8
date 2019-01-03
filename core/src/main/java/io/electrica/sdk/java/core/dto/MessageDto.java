package io.electrica.sdk.java.core.dto;

import com.google.gson.JsonElement;
import io.electrica.sdk.java.api.http.Message;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nullable;
import java.util.UUID;

@Getter
@Setter
public class MessageDto {

    private UUID id;

    private UUID webhookId;

    private UUID webhookServiceId;

    private String name;

    private Long organizationId;

    private Long userId;

    private Long accessKeyId;

    private Message.Scope scope;

    @Nullable
    private Long connectorId;

    @Nullable
    private String connectorErn;

    @Nullable
    private Long connectionId;

    @Nullable
    private JsonElement properties;

    private Boolean expectedResult;

    private JsonElement payload;

}
