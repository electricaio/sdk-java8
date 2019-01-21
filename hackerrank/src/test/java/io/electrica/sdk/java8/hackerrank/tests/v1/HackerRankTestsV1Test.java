package io.electrica.sdk.java8.hackerrank.tests.v1;

import com.google.gson.Gson;
import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.api.http.HttpModule;
import io.electrica.sdk.java8.core.SingleInstanceHttpModule;
import io.electrica.sdk.java8.core.dto.ConnectionDto;
import io.electrica.sdk.java8.core.message.ResultMessage;
import io.electrica.sdk.java8.hackerrank.tests.v1.model.HackerRankV3TestsIndex;
import io.electrica.sdk.java8.hackerrank.tests.v1.model.HackerRankV3TestsIndexResponse;
import io.electrica.sdk.java8.hackerrank.tests.v1.model.HackerRankV3TestsShowResponse;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static io.electrica.sdk.java8.core.SingleInstanceHttpModule.CONNECTIONS_PATH;
import static io.electrica.sdk.java8.core.SingleInstanceHttpModule.INVOKE_PATH;
import static org.junit.jupiter.api.Assertions.*;

class HackerRankTestsV1Test {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    private static Electrica electrica;
    private static HackerRankTestsV1Manager manager;

    private static final String connectionName = "connection_name";
    private static final String accessKey = "key";
    private static HttpModule httpModule;

    private static MockWebServer server;
    private static String url;


    @BeforeAll
    static void setUp() {
        setUpOkHttp();
        httpModule = new SingleInstanceHttpModule(url);
        electrica = Electrica.instance(httpModule, accessKey);
        manager = new HackerRankTestsV1Manager(electrica, true, TIMEOUT);
    }

    static void setUpOkHttp() {
        server = new MockWebServer();
        MockResponse mockedResponse = new MockResponse();
        mockedResponse.setBody("test");
        server.enqueue(mockedResponse);

        try {
            server.start();
        } catch (IOException e) {
        }
        url = server.url("").toString().replaceAll("/$", "");
    }

    private void setServerDispatcher(Object hackerrank) {
        final Dispatcher dispatcher = new Dispatcher() {

            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                ResultMessage result = new ResultMessage();
                result.setResult(new Gson().toJsonTree(hackerrank));
                result.setSuccess(true);
                if (request.getPath().contains(INVOKE_PATH)) {
                    return new MockResponse().setResponseCode(200).setBody(new Gson().toJson(result));
                } else if (request.getPath().contains(CONNECTIONS_PATH)) {
                    try {
                        return new MockResponse().setResponseCode(200).setBody(new Gson().toJson(createConnection()));
                    } catch (IOException e) {
                    }

                }
                return new MockResponse().setResponseCode(404);
            }
        };
        server.setDispatcher(dispatcher);
    }


    private static List<ConnectionDto> createConnection() throws IOException {
        Long connectionId = 100L;
        Map<String, String> connectionProperties = new HashMap<>();

        ConnectionDto connection = new ConnectionDto();
        connection.setName(connectionName);
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
        assertEquals(manager.getConnector().getErn(), HackerRankTestsV1Manager.ERN);
    }

    @Test
    void notExclusiveManagerTest() throws Exception {
        HackerRankTestsV1Manager m = new HackerRankTestsV1Manager(electrica);
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

        setServerDispatcher(response);

        HackerRankTestsV1 channel = manager.getConnectionByName(connectionName);

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
        setServerDispatcher(response);
        HackerRankTestsV1 channel = manager.getConnectionByName(connectionName);
        HackerRankV3TestsShowResponse result = channel.getTest(123, TIMEOUT, TimeUnit.SECONDS);
        assertEquals(result.getId(), id);
    }

    @Test
    void getAllTestsAsynchronously() throws Exception {
        String id = "hackerrank index id";
        HackerRankV3TestsIndexResponse response = createIndexResponse(id);

        setServerDispatcher(response);
        HackerRankTestsV1 channel = manager.getConnectionByName(connectionName);

        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
        channel.getTests(new Callback<HackerRankV3TestsIndexResponse>() {
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
        setServerDispatcher(response);
        HackerRankTestsV1 channel = manager.getConnectionByName(connectionName);
        HackerRankV3TestsIndexResponse result = channel.getTests(TIMEOUT, TimeUnit.SECONDS);
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

