package io.electrica.sdk.java8.api;

import io.electrica.sdk.java8.api.http.Message;

import java.util.Optional;

@FunctionalInterface
public interface MessageListener {

    /**
     * Invoked when message received.
     * <p>
     * Result ignored when used webhook endpoint, that not expect any result.
     *
     * @param message webhook message
     * @return message handling result to return in webhook or empty optional if result not expected. Following
     * primitive types supported to return: String, Boolean, Integer and Double. For {@link Optional#empty()} plain
     * string '{@code null}' will be returned. Also it's possible to return POJOs.
     */
    Optional<Object> onMessage(Message message);

}
