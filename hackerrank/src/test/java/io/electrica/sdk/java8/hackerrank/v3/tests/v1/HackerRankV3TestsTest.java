package io.electrica.sdk.java8.hackerrank.v3.tests.v1;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.api.http.ConnectionInfo;
import io.electrica.sdk.java8.api.http.HttpModule;
import io.electrica.sdk.java8.api.http.Request;
import io.electrica.sdk.java8.core.dto.ConnectionDto;
import io.electrica.sdk.java8.hackerrank.v3.tests.v1.model.HackerRankV3TestsIndex;
import io.electrica.sdk.java8.hackerrank.v3.tests.v1.model.HackerRankV3TestsIndexResponse;
import io.electrica.sdk.java8.hackerrank.v3.tests.v1.model.HackerRankV3TestsShowResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class HackerRankV3TestsTest {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    private static Electrica electrica;
    private static HackerRankV3TestsManager manager;

    private static final String CONNECTION_NAME = "connection_name";
    private static final String ACCESS_KEY = "key";
    private static HttpModule httpModule;


    @BeforeAll
    static void setUp() throws IOException {
        httpModule = mock(HttpModule.class);
        electrica = Electrica.instance(httpModule, ACCESS_KEY);
        manager = new HackerRankV3TestsManager(electrica, true, TIMEOUT);
        createConnections();
    }

    private <R> void mockInvokeCall(R response) throws IOException {

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

    private static void createConnections() throws IOException {

        Long connectionId = 100L;
        Map<String, String> connectionProperties = new HashMap<>();

        ConnectionInfo info = mock(ConnectionInfo.class);
        when(info.getId()).thenReturn(connectionId);
        when(info.getName()).thenReturn(CONNECTION_NAME);
        when(info.getProperties()).thenReturn(connectionProperties);

        List<ConnectionInfo> connections = Collections.singletonList(info);
        doReturn(connections)
                .when(httpModule)
                .getConnections(eq(electrica.getInstanceId()), eq(CONNECTION_NAME), eq(HackerRankV3TestsManager.ERN));
        doReturn(connections)
                .when(httpModule)
                .getConnections(eq(electrica.getInstanceId()), isNull(), eq(HackerRankV3TestsManager.ERN));

    }

    private static List<ConnectionDto> createConnection() throws IOException {
        Long connectionId = 100L;
        Map<String, String> connectionProperties = new HashMap<>();

        ConnectionDto connection = new ConnectionDto();
        connection.setName(CONNECTION_NAME);
        connection.setId(connectionId);
        connection.setProperties(connectionProperties);
        return Collections.singletonList(connection);
    }

    private HackerRankV3TestsShowResponse showResponse() {
        return new HackerRankV3TestsShowResponse();
    }

    @AfterAll
    static void tearDown() throws Exception {
        Connector connector = manager.getConnector();

        assertFalse(connector.isClosed());
        assertFalse(electrica.isClosed());

        manager.close();

        manager.close();

        assertTrue(electrica.isClosed(), "Exclusive manager have to close Electrica instance");
        assertTrue(connector.isClosed(), "Must be closed as part of Electrica instance");
    }

    @Test
    void getConnector() {
        assertEquals(manager.getConnector().getErn(), HackerRankV3TestsManager.ERN);
    }

    @Test
    void notExclusiveManagerTest() throws Exception {
        HackerRankV3TestsManager m = new HackerRankV3TestsManager(electrica);
        Connector connector = m.getConnector();

        assertFalse(m.isExclusive());
        assertNotNull(m.getConnector());

        assertFalse(connector.isClosed());
        assertFalse(electrica.isClosed());

        m.close();

        assertTrue(connector.isClosed());
        assertFalse(electrica.isClosed());
    }

    @Test
    void getIndividualTestAsynchronous() throws Exception {
        HackerRankV3TestsShowResponse response = new HackerRankV3TestsShowResponse();
        String id = "hacker rank show response";
        response.setId(id);

        mockInvokeCall(response);

        HackerRankV3Tests channel = manager.getConnectionByName(CONNECTION_NAME);

        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
        channel.getTest(123, new Callback<HackerRankV3TestsShowResponse>() {
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
        HackerRankV3Tests channel = manager.getConnectionByName(CONNECTION_NAME);
        HackerRankV3TestsShowResponse result = channel.getTest(123, TIMEOUT, TimeUnit.SECONDS);
        assertEquals(result.getId(), id);
    }

    @Test
    void getAllTestsAsynchronously() throws Exception {
        String id = "hackerrank index id";
        HackerRankV3TestsIndexResponse response = createIndexResponse(id);

        mockInvokeCall(response);
        HackerRankV3Tests channel = manager.getConnectionByName(CONNECTION_NAME);

        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
        channel.getOne(new Callback<HackerRankV3TestsIndexResponse>() {
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
        HackerRankV3Tests channel = manager.getConnectionByName(CONNECTION_NAME);
        HackerRankV3TestsIndexResponse result = channel.getOne(TIMEOUT, TimeUnit.SECONDS);
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

