package io.electrica.sdk.java8.api;

import io.electrica.sdk.java8.api.http.Message;

import javax.annotation.Nullable;

@FunctionalInterface
public interface MessageListener {

    /**
     * Invoked when message received.
     * <p>
     * Result ignored when used webhook endpoint, that not expect any result.
     *
     * @param message webhook message
     * @return message handling result string to return in webhook or null if result not expected.
     */
    @Nullable
    String onMessage(Message message);

}
