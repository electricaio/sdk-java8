package io.electrica.sdk.java.api.impl;

import io.electrica.sdk.java.api.Connector;
import io.electrica.sdk.java.api.Electrica;
import io.electrica.sdk.java.api.MessageListener;
import io.electrica.sdk.java.api.http.HttpModule;
import io.electrica.sdk.java.api.http.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * {@link Electrica implementation}
 * <p>
 * Contains the provided access key and,
 * for now - it creates a random client instance id.
 */
@Slf4j
public class ElectricaImpl implements Electrica {

    private final HttpModule httpModule;

    private final UUID instanceId;
    private final String accessKey;

    private final Set<ConnectorImpl> connectors = new HashSet<>();
    private boolean closed = false;

    public ElectricaImpl(HttpModule httpModule, String accessKey) {
        this.httpModule = requireNonNull(httpModule, "httpModule");
        this.accessKey = requireNonNull(accessKey, "accessKey");
        this.instanceId = UUID.randomUUID();
        initialize();
    }

    private synchronized void checkClosed() {
        if (closed) {
            throw new IllegalStateException("Instance has been closed");
        }
    }

    protected void initialize() {
        httpModule.initialize(instanceId, accessKey);
    }

    @Override
    public synchronized Connector connector(String ern) {
        checkClosed();

        ConnectorImpl connector = new ConnectorImpl(this, ern);
        connectors.add(connector);
        return connector;
    }

    @Override
    public synchronized Set<Connector> getConnectors() {
        checkClosed();

        return new HashSet<>(connectors);
    }

    @Override
    public HttpModule getHttpModule() {
        return httpModule;
    }

    @Override
    public UUID getInstanceId() {
        return instanceId;
    }

    @Override
    public String getAccessKey() {
        return accessKey;
    }

    @Override
    public UUID addMessageListener(Predicate<Message> filter, MessageListener listener) {
        checkClosed();

        return httpModule.addMessageListener(instanceId, filter, message -> {
            try {
                Optional<Object> result = listener.onMessage(message);
                if (message.getExpectedResult()) {
                    httpModule.sendMessageResult(instanceId, message, result.orElse(null));
                }
            } catch (Exception e) {
                log.error("Error handling message", e);
            }
        });
    }

    @Override
    public void removeMessageListener(UUID listenerId) {
        checkClosed();

        httpModule.removeMessageListener(instanceId, listenerId);
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void close() throws Exception {
        if (!closed) {
            for (ConnectorImpl connector : connectors) {
                connector.close(true);
            }
            connectors.clear();

            // clear ALL listeners for instance
            httpModule.close(instanceId);
            closed = true;
        }
    }

}
