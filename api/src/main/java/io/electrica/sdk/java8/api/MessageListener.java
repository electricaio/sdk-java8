package io.electrica.sdk.java8.api;

import io.electrica.sdk.java8.api.http.Message;

import java.util.Optional;

public interface MessageListener {

    /**
     * Invoked when message received.
     * <p>
     * Result ignored if used webhook endpoint, that not expect any result.
     *
     * @param message webhook message
     * @return message handling result to return in webhook or empty optional.
     */
    Optional<Object> onMessage(Message message);

}
