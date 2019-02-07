package io.electrica.sdk.java8.core.dto;

import com.google.gson.JsonElement;
import io.electrica.sdk.java8.api.http.Message;
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

    private Boolean isPublic;

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

    /**
     * MediaType according to 'Accept' header. Null if expected result is false.
     */
    @Nullable
    private String expectedContentType;

    private String payload;

    /**
     * MediaType according to 'Content-Type' header.
     */
    private String contentType;
}
