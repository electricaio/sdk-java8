package io.electrica.sdk.java.slack.channel.v1;

import io.electrica.sdk.java.api.Callback;
import io.electrica.sdk.java.api.Connection;
import io.electrica.sdk.java.api.exception.IntegrationException;
import io.electrica.sdk.java.slack.channel.v1.model.SlackChannelV1Action;
import io.electrica.sdk.java.slack.channel.v1.model.SlackChannelV2SendTextPayload;
import lombok.Getter;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Getter
@ThreadSafe
public class SlackChannelV1 implements AutoCloseable {

    public static final String CHANNEL_NAME_PROPERTY_KEY = "channel.name";

    private final Connection connection;
    private final long defaultTimeout;

    public SlackChannelV1(Connection connection) {
        this(connection, TimeUnit.SECONDS.toMillis(60));
    }

    public SlackChannelV1(Connection connection, long defaultTimeout) {
        this.connection = connection;
        this.defaultTimeout = defaultTimeout;
    }

    public void send(String message) throws IntegrationException, IOException, TimeoutException {
        send(message, defaultTimeout, TimeUnit.SECONDS);
    }

    public void send(String message, long timeout, TimeUnit unit)
            throws IntegrationException, IOException, TimeoutException {
        connection.invoke(
                SlackChannelV1Action.SENDTEXT,
                null,
                new SlackChannelV2SendTextPayload().message(message),
                timeout,
                unit
        );
    }

    public void submit(String message, Callback<Void> callback) throws IOException {
        connection.submit(
                SlackChannelV1Action.SENDTEXT,
                null,
                new SlackChannelV2SendTextPayload().message(message),
                callback
        );
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
