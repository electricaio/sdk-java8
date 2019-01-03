package io.electrica.sdk.java8.core.dto;

import io.electrica.sdk.java8.api.http.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MessageResultDto {

    private final UUID messageId;
    private final UUID webhookId;
    private final UUID sdkInstanceId;
    private final UUID webhookServiceId;
    private final Object payload;

    public static MessageResultDto of(UUID instanceId, Message message, @Nullable Object payload) {
        return new MessageResultDto(
                message.getId(),
                message.getWebhookId(),
                instanceId,
                message.getWebhookServiceId(),
                payload
        );
    }

}
