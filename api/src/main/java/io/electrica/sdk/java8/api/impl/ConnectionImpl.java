package io.electrica.sdk.java8.api.impl;

import io.electrica.sdk.java8.api.*;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.api.http.ConnectionInfo;
import io.electrica.sdk.java8.api.http.HttpModule;
import io.electrica.sdk.java8.api.http.Message;
import io.electrica.sdk.java8.api.http.Request;
import lombok.SneakyThrows;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * {@link Connection implementation}.
 * <p>
 * Implements {@link Invocable} contract and executes methods via {@link HttpModule}.
 * <p>
 * Contains information about the {@link Connector} and connection/tenant id.
 */
public class ConnectionImpl implements Connection {

    private final Electrica electrica;
    private final Connector connector;
    private final ConnectionInfo connectionInfo;

    // instance synchronized
    private final List<UUID> listeners = new ArrayList<>();
    private boolean closed = false;

    public ConnectionImpl(Connector connector, ConnectionInfo connectionInfo) {
        this.electrica = connector.getElectrica();
        this.connector = connector;
        this.connectionInfo = connectionInfo;
    }

    private synchronized void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Connection has been closed");
        }
    }

    @Override
    public Connector getConnector() {
        return connector;
    }

    @Override
    public Long getId() {
        return connectionInfo.getId();
    }

    @Override
    public String getName() {
        return connectionInfo.getName();
    }

    @Override
    public synchronized UUID addMessageListener(Predicate<Message> filter, MessageListener listener) {
        checkClosed();

        Predicate<Message> connectionFilter = m ->
                m.getScope() == Message.Scope.Connection && Objects.equals(getId(), m.getConnectionId());

        UUID id = electrica.addMessageListener(connectionFilter.and(filter), listener);
        listeners.add(id);
        return id;
    }

    @Override
    public synchronized void removeMessageListener(UUID listenerId) {
        checkClosed();

        electrica.removeMessageListener(listenerId);
        listeners.remove(listenerId);
    }

    private Map<String, String> getProperties() {
        Map<String, String> properties = connectionInfo.getProperties();
        return properties == null ? Collections.emptyMap() : properties;
    }

    @Override
    public boolean contains(String key) {
        return getProperties().containsKey(key);
    }

    @Nullable
    @Override
    public String getString(String key) {
        return getProperties().get(key);
    }

    @Override
    public <R> R invoke(
            Class<R> resultType,
            Object action,
            @Nullable Object parameters,
            @Nullable Object payload,
            Long timeout,
            TimeUnit unit
    ) throws IntegrationException, IOException, TimeoutException {
        checkClosed();

        SyncCallback<R> callback = new SyncCallback<>();
        submit(resultType, action, parameters, payload, callback);

        Object response = callback.awaitResponse(timeout, unit);
        if (response instanceof IntegrationException) {
            throw ((IntegrationException) response);
        }
        //noinspection unchecked
        return (R) response;
    }

    @Override
    public <R> void submit(
            Class<R> resultType,
            Object action,
            @Nullable Object parameters,
            @Nullable Object payload,
            Callback<R> callback
    ) throws IOException {
        checkClosed();

        Request request = new Request(electrica.getInstanceId(), getId(), action.toString(), parameters, payload);
        electrica.getHttpModule().submitJob(electrica.getInstanceId(), request, resultType, callback);
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        close(false);
    }

    synchronized void close(boolean total) {
        if (!closed) {
            if (!total) {
                for (UUID id : listeners) {
                    electrica.getHttpModule().removeMessageListener(electrica.getInstanceId(), id);
                }
            }
            listeners.clear();

            closed = true;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectionImpl that = (ConnectionImpl) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    private static class SyncCallback<T> implements Callback<T> {

        private final BlockingQueue<Object[]> responseQueue = new ArrayBlockingQueue<>(1);

        @Override
        public void onResponse(T result) {
            responseQueue.add(new Object[]{result});
        }

        @Override
        public void onFailure(IntegrationException exception) {
            responseQueue.add(new Object[]{exception});
        }

        @SneakyThrows
        private Object awaitResponse(Long timeout, TimeUnit unit) throws TimeoutException {
            Object[] responseContainer = responseQueue.poll(timeout, unit);
            if (responseContainer == null) {
                throw new TimeoutException();
            }
            return responseContainer[0];
        }
    }
}
