package io.electrica.sdk.java8.hackerrank.v3.tests.v1;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.hackerrank.v3.tests.v1.model.*;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ThreadSafe
public class HackerRankV3Tests implements AutoCloseable {

    public static final String ERN = "ern://hackerrank-v3:tests:1";

    private final Connection connection;

    public HackerRankV3Tests(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Asynchronously fetch all tests for a connection from the HackerRank API.
     * <p>
     *
     * @param callback Handling async call and on success <code>HackerRankV3TestsIndexResponse</code> is returned
     * @throws IOException
     */
    public void getAll(Callback<HackerRankV3TestsIndexResponse> callback, int limit, int offset) throws IOException {
        connection.submit(
                HackerRankV3TestsIndexResponse.class,
                HackerRankV3TestsAction.TESTSINDEX,
                null,
                new LimitOffset().limit(limit).offset(offset),
                callback
        );
    }

    /**
     * Synchronously fetch all tests for a connection from the HackerRank API.
     *
     * @param timeout The amount of time to wait on HackerRank to return the result
     * @param unit    The unit of time to wait on HackerRank to return the result
     * @return
     * @throws IOException
     * @throws TimeoutException
     * @throws IntegrationException
     */
    public HackerRankV3TestsIndexResponse getAll(long timeout, TimeUnit unit, int limit, int offset) throws
            IOException, TimeoutException,
            IntegrationException {
        return connection.invoke(
                HackerRankV3TestsIndexResponse.class,
                HackerRankV3TestsAction.TESTSINDEX,
                null,
                new LimitOffset().limit(limit).offset(offset),
                timeout,
                unit
        );
    }

    /**
     * Asynchronously fetch an individual test from the HackerRank API.
     * <p>
     *
     * @param id       The test id
     * @param callback Handling async call and on success <code>HackerRankV3TestsIndexResponse</code> is returned
     * @throws IOException
     */
    public void getOne(int id, Callback<HackerRankV3TestsShowResponse> callback) throws IOException {
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
     *
     * @param id      The test ID
     * @param timeout The amount of time to wait on HackerRank to return the result
     * @param unit    The unit of time to wait on HackerRank to return the result
     * @return
     * @throws IOException
     * @throws TimeoutException
     * @throws IntegrationException
     */
    public HackerRankV3TestsShowResponse getOne(int id, long timeout, TimeUnit unit) throws IOException,
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
