package io.electrica.sdk.java.api;

import io.electrica.sdk.java.api.http.Message;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Raw Connection contract via which we can execute the "invoke api" methods.
 */
public interface Connection extends Invocable, ConnectionProperties, AutoCloseable {

    Connector getConnector();

    Long getId();

    String getName();

    /**
     * Add message listener for this particular connection.
     */
    UUID addMessageListener(Predicate<Message> filter, MessageListener listener);

    void removeMessageListener(UUID listenerId);

    boolean isClosed();

}
