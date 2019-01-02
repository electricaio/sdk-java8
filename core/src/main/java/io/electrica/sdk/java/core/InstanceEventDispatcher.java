package io.electrica.sdk.java.core;

import com.google.gson.Gson;
import io.electrica.sdk.java.api.http.Message;
import io.electrica.sdk.java.core.dto.MessageDto;
import io.electrica.sdk.java.core.dto.MessageImpl;
import io.electrica.sdk.java.core.message.InboundMessage;
import io.electrica.sdk.java.core.message.WebhookInboundMessage;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
public class InstanceEventDispatcher implements AutoCloseable {

    protected final Gson gson;
    protected final ExecutorService executor;
    protected final long terminationTimeoutMillis;
    protected final List<MessageListener> messageListeners = new ArrayList<>();

    protected InstanceEventDispatcher(Gson gson, ExecutorService executor, long terminationTimeoutMillis) {
        this.gson = gson;
        this.executor = executor;
        this.terminationTimeoutMillis = terminationTimeoutMillis;
    }

    public UUID addMessageListener(Predicate<Message> filter, Consumer<Message> listener) {
        UUID id = UUID.randomUUID();
        synchronized (messageListeners) {
            messageListeners.add(new MessageListener(id, filter, listener));
        }
        return id;
    }

    public void removeMessageListener(UUID id) {
        synchronized (messageListeners) {
            messageListeners.removeIf(listener -> Objects.equals(id, listener.id));
        }
    }

    protected void submit(InboundMessage message, WebSocketHandler.AckSender ackSender) {
        if (message instanceof WebhookInboundMessage) {
            MessageDto messageDto = ((WebhookInboundMessage) message).getData();
            executor.submit(() -> {
                boolean ackSent = false;
                synchronized (messageListeners) {
                    for (MessageListener listener : messageListeners) {
                        Message userMessage = new MessageImpl(gson, messageDto);
                        if (listener.filter.test(userMessage)) {
                            if (!ackSent) {
                                // Send accepted ack if at least one listener found
                                ackSender.send(true);
                                ackSent = true;
                            }
                            // submit message consume task
                            executor.submit(() -> {
                                try {
                                    listener.consumer.accept(userMessage);
                                } catch (Exception e) {
                                    log.error("Unhandled onMessage() exception", e);
                                }
                            });
                        }
                    }
                }
                if (!ackSent) {
                    // Send nack if no one listener found
                    ackSender.send(false);
                }
            });
        } else {
            throw new UnsupportedOperationException("Unsupported message type: " + message);
        }
    }

    @Override
    public void close() {
        shutdownExecutor();
        messageListeners.clear();
    }

    private void shutdownExecutor() {
        executor.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!executor.awaitTermination(terminationTimeoutMillis, TimeUnit.MILLISECONDS)) {
                List<Runnable> tasks = executor.shutdownNow(); // Cancel currently executing tasks
                log.error("Dropped {} enqueued event dispatcher tasks", tasks.size());
                log.info("Awaiting termination of executing event dispatcher tasks");
                if (!executor.awaitTermination(terminationTimeoutMillis, TimeUnit.MILLISECONDS)) {
                    log.error("Can't await executing tasks termination. " +
                            "Please consider increase termination timeout parameter");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            executor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    @AllArgsConstructor
    private static class MessageListener {
        private final UUID id;
        private final Predicate<Message> filter;
        private final Consumer<Message> consumer;
    }
}
