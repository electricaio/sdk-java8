package io.electrica.sdk.java8.api;

import io.electrica.sdk.java8.api.exception.ConnectionNotFoundException;
import io.electrica.sdk.java8.api.http.Message;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Connector contract.
 * <p>
 * Contains information such as:
 * <p>
 * - client instance id
 * - access key
 * - ern
 */
public interface Connector extends AutoCloseable {

    Electrica getElectrica();

    String getErn();

    Set<Connection> getConnections();

    Connection connection(String name) throws ConnectionNotFoundException;

    Connection defaultConnection() throws ConnectionNotFoundException;

    List<Connection> allConnections() throws ConnectionNotFoundException;

    /**
     * Add message listener for this particular connector.
     */
    UUID addMessageListener(Predicate<Message> filter, MessageListener listener);

    void removeMessageListener(UUID listenerId);

    boolean isClosed();

}
