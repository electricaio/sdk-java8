package io.electrica.sdk.java8.core;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Interface to specify retry strategy behaviour.
 */
public interface RetryStrategy {

    /**
     * Get delay for specified number of retry.
     *
     * @param retryNumber number of retry, e.q 1 mean first retry after 1 failure
     * @return delay in millis before the next retry or {@link Optional#empty()} to give up
     */
    Optional<Long> getDelay(int retryNumber);

    class Linear implements RetryStrategy {

        private final int maxRetries;
        private final long delay;

        public Linear(int maxRetries, long delay, TimeUnit unit) {
            this.maxRetries = maxRetries;
            this.delay = unit.toMillis(delay);
        }

        @Override
        public Optional<Long> getDelay(int retryNumber) {
            return retryNumber > maxRetries ? Optional.empty() : Optional.of(delay);
        }
    }
}
