package io.electrica.sdk.java.api.http;

import io.electrica.sdk.java.api.Callback;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface HttpModule extends AutoCloseable {

    void initialize(UUID instanceId, String accessKey);

    <R> void submitJob(
            UUID instanceId,
            Request request,
            Class<R> resultType,
            Callback<R> callback
    ) throws IOException;

    List<ConnectionInfo> getConnections(UUID instanceId, @Nullable String name, String ern) throws IOException;

    void sendMessageResult(UUID instanceId, Message message, @Nullable Object result) throws IOException;

    UUID addMessageListener(UUID instanceId, Predicate<Message> filter, Consumer<Message> listener);

    void removeMessageListener(UUID instanceId, UUID listenerId);

    void close(UUID instanceId) throws Exception;

}
