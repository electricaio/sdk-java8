package io.electrica.sdk.java.api;

import io.electrica.sdk.java.api.exception.IntegrationException;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Contract that defines invoke api methods.
 */
public interface Invocable {

    /**
     * Invoke Electrica.io API to start specified action and return result of expected type synchronously.
     *
     * @param <R>        result type or {@link Void} if no result expected
     * @param resultType expected type of result, specify {@link Void} if nothing expected
     * @param action     action identifier
     * @param parameters action parameters
     * @param payload    action payload
     * @param timeout    how long to wait before giving up, in units of {@code unit}
     * @param unit       a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @return result of action execution or {@code null} if {@link Void} result type has been specified
     * @throws IntegrationException if any errors occur during integration job execution
     * @throws IOException          if any network errors occur
     * @throws TimeoutException     if no result provided after specified {@code timeout}
     */
    <R> R invoke(
            Class<R> resultType,
            Object action,
            @Nullable Object parameters,
            @Nullable Object payload,
            Long timeout,
            TimeUnit unit
    ) throws IntegrationException, IOException, TimeoutException;

    /**
     * Invoke Electrica.io API to start specified action without result synchronously.
     *
     * @param action     action identifier
     * @param parameters action parameters
     * @param payload    action payload
     * @param timeout    how long to wait before giving up, in units of {@code unit}
     * @param unit       a {@link TimeUnit} determining how to interpret the {@code timeout} parameter
     * @throws IntegrationException if any errors occur during integration job execution
     * @throws IOException          if any network errors occur
     * @throws TimeoutException     if no result provided after specified {@code timeout}
     */
    default void invoke(
            Object action,
            @Nullable Object parameters,
            @Nullable Object payload,
            Long timeout,
            TimeUnit unit
    ) throws IntegrationException, IOException, TimeoutException {
        invoke(Void.class, action, parameters, payload, timeout, unit);
    }

    /**
     * Invoke Electrica.io API to start specified action and return result of expected type asynchronously.
     *
     * @param <R>        result type or {@link Void} if no result expected
     * @param resultType expected type of result, specify {@link Void} if nothing expected
     * @param action     action identifier
     * @param parameters action parameters
     * @param payload    action payload
     * @param callback   callback that responsible for response handling
     * @throws IOException if any network errors occur
     */
    <R> void submit(
            Class<R> resultType,
            Object action,
            @Nullable Object parameters,
            @Nullable Object payload,
            Callback<R> callback
    ) throws IOException;

    /**
     * Invoke Electrica.io API to start action without result asynchronously.
     *
     * @param action     action identifier
     * @param parameters action parameters
     * @param payload    action payload
     * @param callback   listener that responsible for result handling
     * @throws IOException if any network errors occur
     */
    default void submit(
            Object action,
            @Nullable Object parameters,
            @Nullable Object payload,
            Callback<Void> callback
    ) throws IOException {
        submit(Void.class, action, parameters, payload, callback);
    }

}
