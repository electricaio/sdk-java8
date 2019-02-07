package io.electrica.sdk.java8.api;

import io.electrica.sdk.java8.api.exception.IntegrationException;

import java.util.function.Function;

/**
 * Interface to get integration job result in asynchronous manner.
 *
 * @param <E> type of integration job result
 */
public interface Callback<E> {

    /**
     * Invoked when integration job result received.
     *
     * @param result typed result or {@code null} if nothing expected from integration job
     */
    void onResponse(E result);

    /**
     * Invoked if any errors occur during integration job execution.
     *
     * @param exception exception object
     */
    void onFailure(IntegrationException exception);

    /**
     * Create callback, adopted to listen another type and convert execution result to current type.
     * <p>
     * <p>Typically used to handle results in type, other than Connector action result:
     * <pre>{@code
     *
     *   Callback<String> echoAnswerCallback = new Callback<String>() {
     *       public void onResponse(String answer) {
     *           ...
     *       }
     *
     *       public void onFailure(IntegrationException exception) {
     *           ...
     *       }
     *   };
     *
     *   connection.submit(
     *           EchoResult.class,
     *           "echo",
     *           new EchoParameters(),
     *           new EchoPayload().message("Echo Message"),
     *           echoAnswerCallback.adapted(EchoResult::getAnswer)
     *   );
     * }</pre>
     *
     * @param adapter function to convert one type to another
     * @param <T>     type of adapted handler
     * @return response handler of adapted type
     */
    default <T> Callback<T> adapted(Function<T, E> adapter) {
        return new Callback<T>() {
            @Override
            public void onResponse(T result) {
                Callback.this.onResponse(adapter.apply(result));
            }

            @Override
            public void onFailure(IntegrationException exception) {
                Callback.this.onFailure(exception);
            }
        };
    }

}
