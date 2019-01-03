package io.electrica.sdk.java8.echo.test.v1;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.echo.test.v1.model.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class EchoTestV1 implements AutoCloseable {

    private final Connection connection;

    public EchoTestV1(Connection connection) {
        this.connection = connection;
    }

    public void ping() throws IntegrationException, IOException, TimeoutException {
        ping(false);
    }

    public void ping(boolean throwException) throws IntegrationException, IOException, TimeoutException {
        ping(throwException, 60L, TimeUnit.SECONDS);
    }

    public void ping(long timeout, TimeUnit unit) throws IntegrationException, IOException, TimeoutException {
        ping(false, timeout, unit);
    }

    public void ping(boolean throwException, long timeout, TimeUnit unit)
            throws IntegrationException, IOException, TimeoutException {
        connection.invoke(
                EchoTestV1Action.PING,
                new EchoTestV1PingParameters().throwException(throwException),
                null,
                timeout,
                unit
        );
    }

    public void asyncPing(Callback<Void> callback) throws IOException {
        asyncPing(false, callback);
    }

    public void asyncPing(boolean throwException, Callback<Void> callback) throws IOException {
        connection.submit(
                EchoTestV1Action.PING,
                new EchoTestV1PingParameters().throwException(throwException),
                null,
                callback
        );
    }

    public String echo(String message) throws IntegrationException, IOException, TimeoutException {
        return echo(message, false);
    }

    public String echo(String message, boolean throwException)
            throws IntegrationException, IOException, TimeoutException {
        return echo(message, throwException, 60L, TimeUnit.SECONDS);
    }

    public String echo(String message, long timeout, TimeUnit unit)
            throws IntegrationException, IOException, TimeoutException {
        return echo(message, false, timeout, unit);
    }

    public String echo(String message, boolean throwException, long timeout, TimeUnit unit)
            throws IntegrationException, IOException, TimeoutException {
        return connection.invoke(
                EchoTestV1SendResult.class,
                EchoTestV1Action.SEND,
                new EchoTestV1SendParameters().throwException(throwException),
                new EchoTestV1SendPayload().message(message),
                timeout,
                unit
        ).getMessage();
    }

    public void asyncEcho(String message, Callback<String> resultHandler) throws IOException {
        asyncEcho(message, false, resultHandler);
    }

    public void asyncEcho(
            String message,
            boolean throwException,
            Callback<String> resultHandler
    ) throws IOException {
        connection.submit(
                EchoTestV1SendResult.class,
                EchoTestV1Action.SEND,
                new EchoTestV1SendParameters().throwException(throwException),
                new EchoTestV1SendPayload().message(message),
                resultHandler.adapted(EchoTestV1SendResult::getMessage)
        );
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
