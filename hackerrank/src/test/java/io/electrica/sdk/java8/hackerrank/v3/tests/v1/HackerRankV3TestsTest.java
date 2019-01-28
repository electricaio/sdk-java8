package io.electrica.sdk.java8.hackerrank.v3.tests.v1;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.api.http.ConnectionInfo;
import io.electrica.sdk.java8.api.http.HttpModule;
import io.electrica.sdk.java8.api.http.Request;
import io.electrica.sdk.java8.hackerrank.v3.tests.v1.model.HackerRankV3TestsIndex;
import io.electrica.sdk.java8.hackerrank.v3.tests.v1.model.HackerRankV3TestsIndexResponse;
import io.electrica.sdk.java8.hackerrank.v3.tests.v1.model.HackerRankV3TestsShowResponse;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HackerRankV3TestsTest {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private static Electrica electrica;
    private static HttpModule httpModule = mock(HttpModule.class);
    private static HackerRankV3Tests tests;

    @BeforeAll
    static void setUp() throws IOException {
        electrica = Electrica.instance(httpModule, "key");
        mockConnection();
    }

    @AfterAll
    static void tearDown() throws Exception {
        electrica.close();
    }

    private static void mockConnection() throws IOException {
        doAnswer(invocation -> {
            ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
            when(connectionInfo.getId()).thenReturn(1L);
            when(connectionInfo.getName()).thenReturn(Connection.DEFAULT_NAME);
            return Collections.singletonList(connectionInfo);
        })
                .when(httpModule)
                .getConnections(
                        eq(electrica.getInstanceId()),
                        eq(Connection.DEFAULT_NAME),
                        eq(HackerRankV3Tests.ERN)
                );
    }

    private static <R> void mockInvokeCall(R response) throws IOException {

        doAnswer(invocation -> {
            Callback<R> rh = invocation.getArgument(3);
            rh.onResponse(response);
            return null;
        })
                .when(httpModule)
                .submitJob(
                        eq(electrica.getInstanceId()),
                        any(Request.class),
                        any(),
                        ArgumentMatchers.<Callback<R>>any());

    }

    @BeforeEach
    void setUpHackerRank() {
        Connector connector = electrica.connector(HackerRankV3Tests.ERN);
        Connection connection = connector.defaultConnection();
        tests = new HackerRankV3Tests(connection);
    }

    @AfterEach
    void tearDownHackerRank() throws Exception {
        tests.close();
        assertTrue(tests.getConnection().isClosed());
    }

    @Test
    void getIndividualTestAsynchronous() throws Exception {
        HackerRankV3TestsShowResponse response = new HackerRankV3TestsShowResponse();
        String id = "hacker rank show response";
        response.setId(id);

        mockInvokeCall(response);

        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
        tests.getOne(123, new Callback<HackerRankV3TestsShowResponse>() {
            @Override
            public void onResponse(HackerRankV3TestsShowResponse result) {
                queue.add(result);
            }

            @Override
            public void onFailure(IntegrationException exception) {
                queue.add(exception);
            }
        });

        HackerRankV3TestsShowResponse result = (HackerRankV3TestsShowResponse) queue.poll(TIMEOUT,
                TimeUnit.MILLISECONDS);
        assertEquals(result.getId(), id);
    }

    @Test
    void getIndividualTestSynchronously() throws Exception {
        String id = "hacker rank show response";
        HackerRankV3TestsShowResponse response = new HackerRankV3TestsShowResponse();
        response.setId(id);
        mockInvokeCall(response);
        HackerRankV3TestsShowResponse result = tests.getOne(123, TIMEOUT, TimeUnit.SECONDS);
        assertEquals(result.getId(), id);
    }

    @Test
    void getAllTestsAsynchronously() throws Exception {
        String id = "hackerrank index id";
        HackerRankV3TestsIndexResponse response = createIndexResponse(id);

        mockInvokeCall(response);

        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
        tests.getAll(new Callback<HackerRankV3TestsIndexResponse>() {
            @Override
            public void onResponse(HackerRankV3TestsIndexResponse result) {
                queue.add(result);
            }

            @Override
            public void onFailure(IntegrationException exception) {
                queue.add(exception);
            }
        });

        HackerRankV3TestsIndexResponse result = (HackerRankV3TestsIndexResponse) queue.poll(TIMEOUT,
                TimeUnit.MILLISECONDS);
        assertEquals(result.getData().get(0).getId(), id);
    }

    @Test
    void getAllTestsSynchronously() throws Exception {
        String id = "hackerrank index id";
        HackerRankV3TestsIndexResponse response = createIndexResponse(id);
        mockInvokeCall(response);
        HackerRankV3TestsIndexResponse result = tests.getAll(TIMEOUT, TimeUnit.SECONDS);
        assertEquals(result.getData().get(0).getId(), id);
    }

    private HackerRankV3TestsIndexResponse createIndexResponse(String id) {
        HackerRankV3TestsIndexResponse response = new HackerRankV3TestsIndexResponse();
        HackerRankV3TestsIndex index = new HackerRankV3TestsIndex();
        index.setId(id);
        response.setData(Collections.singletonList(index));
        return response;
    }
}
