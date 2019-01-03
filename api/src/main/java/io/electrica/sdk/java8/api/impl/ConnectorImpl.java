package io.electrica.sdk.java8.api.impl;

import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.MessageListener;
import io.electrica.sdk.java8.api.exception.ConnectionNotFoundException;
import io.electrica.sdk.java8.api.http.ConnectionInfo;
import io.electrica.sdk.java8.api.http.Message;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * An implementation of {@link Connector}.
 */
public final class ConnectorImpl implements Connector {

    static final String DEFAULT_CONNECTION = "Default";

    private final Electrica electrica;
    private final String ern;

    // instance synchronized
    private final List<UUID> listeners = new ArrayList<>();
    private final Set<ConnectionImpl> connections = new HashSet<>();
    private boolean closed = false;

    ConnectorImpl(Electrica electrica, String ern) {
        this.electrica = electrica;
        this.ern = requireNonNull(ern).toLowerCase();
    }

    private synchronized void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Connector has been closed");
        }
    }

    @Override
    public Electrica getElectrica() {
        return electrica;
    }

    @Override
    public String getErn() {
        return ern;
    }

    @Override
    public synchronized Set<Connection> getConnections() {
        checkClosed();
        return new HashSet<>(connections);
    }

    @Override
    public Connection connection(String name) {
        return fetchConnectionDtos(name).stream()
                .findFirst()
                .map(this::createConnection)
                .orElseThrow(() -> new ConnectionNotFoundException("Connection not found by name: " + name));
    }

    @Override
    public Connection defaultConnection() {
        return connection(DEFAULT_CONNECTION);
    }

    @Override
    public List<Connection> allConnections() {
        return fetchConnectionDtos(null).stream()
                .map(this::createConnection)
                .collect(Collectors.toList());
    }

    private List<ConnectionInfo> fetchConnectionDtos(@Nullable String name) {
        checkClosed();

        try {
            return electrica.getHttpModule().getConnections(electrica.getInstanceId(), name, ern);
        } catch (IOException e) {
            throw new ConnectionNotFoundException("Errors fetching connections", e);
        }
    }

    @Override
    public synchronized UUID addMessageListener(Predicate<Message> filter, MessageListener listener) {
        checkClosed();

        Predicate<Message> connectorFilter = m ->
                m.getScope() == Message.Scope.Connector && ern.equalsIgnoreCase(m.getConnectorErn());
        UUID id = electrica.addMessageListener(connectorFilter.and(filter), listener);
        listeners.add(id);
        return id;
    }

    @Override
    public synchronized void removeMessageListener(UUID listenerId) {
        checkClosed();

        electrica.removeMessageListener(listenerId);
        listeners.remove(listenerId);
    }

    private synchronized Connection createConnection(ConnectionInfo info) {
        ConnectionImpl connection = new ConnectionImpl(ConnectorImpl.this, info);
        connections.add(connection);
        return connection;
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public void close() throws Exception {
        close(false);
    }

    synchronized void close(boolean total) throws Exception {
        if (!closed) {
            for (ConnectionImpl connection : connections) {
                connection.close(total);
            }
            connections.clear();

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
        ConnectorImpl connector = (ConnectorImpl) o;
        return ern.equals(connector.ern);
    }

    @Override
    public int hashCode() {
        return ern.hashCode();
    }
}
