package io.electrica.sdk.java8.slack.channel.v1;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.slack.channel.v1.model.SlackChannelV1Action;
import io.electrica.sdk.java8.slack.channel.v1.model.SlackChannelV1SendTextPayload;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ThreadSafe
public class SlackChannelV1 implements AutoCloseable {

    public static final String CHANNEL_NAME_PROPERTY_KEY = "channel.name";

    private final Connection connection;
    private final long timeout;

    public SlackChannelV1(Connection connection) {
        this(connection, SlackChannelV1Manager.DEFAULT_TIMEOUT);
    }

    public SlackChannelV1(Connection connection, long timeout) {
        this.connection = connection;
        this.timeout = timeout;
    }

    public Connection getConnection() {
        return connection;
    }

    public long getTimeout() {
        return timeout;
    }

    public void send(String message) throws IntegrationException, IOException, TimeoutException {
        send(message, timeout, TimeUnit.MILLISECONDS);
    }

    public void send(String message, long timeout, TimeUnit unit)
            throws IntegrationException, IOException, TimeoutException {
        connection.invoke(
                SlackChannelV1Action.SENDTEXT,
                null,
                new SlackChannelV1SendTextPayload().message(message),
                timeout,
                unit
        );
    }

    public void submit(String message, Callback<Void> callback) throws IOException {
        connection.submit(
                SlackChannelV1Action.SENDTEXT,
                null,
                new SlackChannelV1SendTextPayload().message(message),
                callback
        );
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
