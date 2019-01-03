package io.electrica.sdk.java.core.dto;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import io.electrica.sdk.java.api.http.Message;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MessageImpl implements Message {

    private final Gson gson;
    private final MessageDto dto;

    @Override
    public UUID getId() {
        return dto.getId();
    }

    @Override
    public UUID getWebhookId() {
        return dto.getWebhookId();
    }

    @Override
    public UUID getWebhookServiceId() {
        return dto.getWebhookServiceId();
    }

    @Override
    public String getName() {
        return dto.getName();
    }

    @Override
    public Scope getScope() {
        return dto.getScope();
    }

    @Nullable
    @Override
    public Long getConnectorId() {
        return dto.getConnectorId();
    }

    @Nullable
    @Override
    public String getConnectorErn() {
        return dto.getConnectorErn();
    }

    @Nullable
    @Override
    public Long getConnectionId() {
        return dto.getConnectionId();
    }

    @Override
    public Map<String, String> getPropertiesMap() {
        JsonElement properties = dto.getProperties();
        return properties == null ?
                Collections.emptyMap() :
                gson.fromJson(properties, new TypeToken<Map<String, String>>() {
                }.getType());
    }

    @Override
    public Boolean getExpectedResult() {
        return dto.getExpectedResult();
    }

    @Override
    public <T> T getPayload(Class<T> type) {
        return gson.fromJson(dto.getPayload(), type);
    }
}
