package io.electrica.sdk.java8.hackerrank.tests.v1;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.hackerrank.tests.v1.model.HackerRankV3TestsAction;
import io.electrica.sdk.java8.hackerrank.tests.v1.model.HackerRankV3TestsIndexResponse;
import io.electrica.sdk.java8.hackerrank.tests.v1.model.HackerRankV3TestsShowPayload;
import io.electrica.sdk.java8.hackerrank.tests.v1.model.HackerRankV3TestsShowResponse;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ThreadSafe
public class HackerRankTestsV1 implements AutoCloseable {

    private final Connection connection;
    private final long timeout;

    public HackerRankTestsV1(Connection connection) {
        this(connection, HackerRankTestsV1Manager.DEFAULT_TIMEOUT);
    }

    public HackerRankTestsV1(Connection connection, long timeout) {
        this.connection = connection;
        this.timeout = timeout;
    }

    public Connection getConnection() {
        return connection;
    }

    public long getTimeout() {
        return timeout;
    }

    /**
     * Asynchronously fetch all tests for a connection from the HackerRank API.
     * <p>
     * @param callback
     * @throws IOException
     */
    public void getTests(Callback<HackerRankV3TestsIndexResponse> callback) throws IOException {
        connection.submit(
                HackerRankV3TestsIndexResponse.class,
                HackerRankV3TestsAction.TESTSINDEX,
                null,
                null,
                callback
        );
    }


    /**
     * Synchronously fetch all tests for a connection from the HackerRank API.
     * @param timeout
     * @param unit
     * @return
     * @throws IOException
     * @throws TimeoutException
     * @throws IntegrationException
     */
    public HackerRankV3TestsIndexResponse getTests(long timeout, TimeUnit unit) throws IOException, TimeoutException,
            IntegrationException {
        return connection.invoke(
                HackerRankV3TestsIndexResponse.class,
                HackerRankV3TestsAction.TESTSINDEX,
                null,
                null,
                timeout,
                unit
        );
    }

    /**
     * Asynchronously fetch an individual test from the HackerRank API.
     * <p>
     * @param id
     * @param callback
     * @throws IOException
     */
    public void getTest(int id, Callback<HackerRankV3TestsShowResponse> callback) throws IOException {
        connection.submit(
                HackerRankV3TestsShowResponse.class,
                HackerRankV3TestsAction.TESTSSHOW,
                null,
                new HackerRankV3TestsShowPayload().id(id),
                callback
        );
    }

    /**
     * Synchronously fetch an individual test from the HackerRank API.
     * <p>
     * @param id
     * @param timeout
     * @param unit
     * @return
     * @throws IOException
     * @throws TimeoutException
     * @throws IntegrationException
     */
    public HackerRankV3TestsShowResponse getTest(int id, long timeout, TimeUnit unit) throws IOException,
            TimeoutException, IntegrationException {
        return connection.invoke(
                HackerRankV3TestsShowResponse.class,
                HackerRankV3TestsAction.TESTSSHOW,
                null,
                new HackerRankV3TestsShowPayload().id(id),
                timeout,
                unit

        );
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
