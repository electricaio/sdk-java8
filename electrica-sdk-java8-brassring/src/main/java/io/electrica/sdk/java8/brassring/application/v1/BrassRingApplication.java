package io.electrica.sdk.java8.brassring.application.v1;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.brassring.application.v1.model.BrassRingApplicationAction;
import io.electrica.sdk.java8.brassring.application.v1.model.BrassRingApplicationPayload;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ThreadSafe
public class BrassRingApplication implements AutoCloseable {

    public static final String ERN = "ern://brassring:application:1";

    private final Connection connection;

    public BrassRingApplication(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Synchronously insert/update forms for a connection into the BrassRing API.
     */
    public void update(BrassRingApplicationPayload context, long timeout, TimeUnit unit)
            throws IOException, TimeoutException, IntegrationException {
        connection.invoke(
                BrassRingApplicationAction.UPDATE,
                null,
                context,
                timeout,
                unit
        );
    }

    /**
     * Asynchronously insert/update forms for a connection into the BrassRing API and provide result to specified
     * callback.
     */
    public void update(BrassRingApplicationPayload context, Callback<Void> callback) throws IOException {
        connection.submit(
                BrassRingApplicationAction.UPDATE,
                null,
                context,
                callback
        );
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
