package io.electrica.sdk.java8.core;

import com.google.gson.Gson;
import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.exception.ConnectionNotFoundException;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.api.http.Message;
import io.electrica.sdk.java8.echo.test.v1.EchoTestV1;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import okhttp3.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SingleInstanceHttpModuleTest {

    private static final String API_URL = "http://api.dev.electrica.io";
    private static final UUID WEBHOOK_ID = UUID.fromString("5766130e-154e-4ce4-b5fd-eefe7afaee40");
    private static final String WEBHOOK_URL = API_URL + "/v1/webhooks/" + WEBHOOK_ID;
    private static final String WEBHOOK_NAME = "Default";
    private static final String ECHO_MESSAGE = "Test echo message";
    private static final String CONNECTION_NOT_FOUND_MESSAGE = "Connection not found by name: ";
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    private static final String ACCESS_KEY = "" +
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsiYyIsInUiLCJjaCIsInciLCJpIiwid3MiXSwidXNlcl9uYW1lIjoiQ" +
            "GlkOjEiLCJzY29wZSI6WyJyIiwic2RrIl0sImV4cCI6MzY5NDQ0NzM3OCwiaWF0IjoxNTQ2OTYzNzMxLCJhdXRob3JpdGllcyI6WyJ" +
            "rZXk6MSIsIm9yZzoyIl0sImp0aSI6IjdkNTdmMjY0LWMwZTctNDgwNS1hMTMxLTgzODhjNzc5MTE5YiIsImNsaWVudF9pZCI6ImFjY" +
            "2Vzc0tleSJ9.a9pw59ENqlTtCgJ04IgrR6Dmzr8xFpqzPTZoqSX2c2pXw-bLfz_ZO62CXO_pbGpxCZY5Ny1I2-dtTXliYEi6E2aUww" +
            "ArTQmPX1M8iWQPFbA_AnvCbm7G_Gdi8QuJrEWz3rEyxpV90xdCoBlWifZb0uYj8F58ElP5O7w5n16oCFu0KeUmvZrDFV0pn-javawn" +
            "HNjOBnGC_gJWImsPgIjCsgn8aWGy0-wuCmGzcYCSOLA9qQ-xiPO6WXrmCLcJUW2m0vO8kp5wMOhK-0soJSm8m3eUucUUY0kgc4IMVF" +
            "iuRoM-uGV5wmSzkN8zCGrL6Am1jwQ9Uxx7VZrPWv3oGqBgBw";

    private static Electrica electrica;
    private static OkHttpClient httpClient;

    private Gson gson = new Gson();

    @BeforeAll
    static void setUp() {
        httpClient = new OkHttpClient.Builder().build();
        SingleInstanceHttpModule httpModule = new SingleInstanceHttpModule(API_URL);
        electrica = Electrica.instance(httpModule, ACCESS_KEY);
    }

    @AfterAll
    static void tearDown() {
        try {
            httpClient.dispatcher().cancelAll();
            electrica.close();
        } catch (Exception e) {
            fail("Electrica instance Close exception", e);
        }
    }

    private static <T> T awaitResultFromQueue(BlockingQueue<T> queue) throws InterruptedException {
        T result = queue.poll(30, TimeUnit.SECONDS);
        assertNotNull(result, "Waiting of result timed out");
        return result;
    }

    private static void assertMessage(Connection connection, Message message, boolean isExpectedResult) {
        assertNotNull(message.getId());
        assertEquals(WEBHOOK_ID, message.getWebhookId());
        assertNotNull(message.getWebhookServiceId());
        assertEquals(WEBHOOK_NAME, message.getName());
        assertEquals(Message.Scope.Connection, message.getScope());
        assertNull(message.getConnectorId());
        assertNull(message.getConnectorErn());
        assertEquals(connection.getId(), message.getConnectionId());
        assertNull(message.getPropertiesMap()); //TODO !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
        assertEquals(isExpectedResult, message.getExpectedResult());
        assertEquals(WebhookMessage.INSTANCE, message.getPayload(WebhookMessage.class));
    }

    @Test
    void wrongConnectorTest() {
        ConnectionNotFoundException e = assertThrows(ConnectionNotFoundException.class, () -> {
            Connector connector = electrica.connector("wrong-connector-ern");
            connector.defaultConnection();
        });
        assertEquals(CONNECTION_NOT_FOUND_MESSAGE + Connection.DEFAULT_NAME, e.getMessage());
    }

    @Test
    void wrongConnectionTest() {
        String connectionWrongName = "wrong-connection-name";
        ConnectionNotFoundException e = assertThrows(ConnectionNotFoundException.class, () -> {
            Connector connector = electrica.connector(EchoTestV1.ERN);

            connector.connection(connectionWrongName);
        });
        assertEquals(CONNECTION_NOT_FOUND_MESSAGE + connectionWrongName, e.getMessage());
    }

    @Test
    void echoPingTest() throws Exception {
        EchoTestV1 echoTestV1 = createEchoTestV1();
        echoTestV1.ping();
    }

    @Test
    void echoSendTest() throws Exception {
        EchoTestV1 echoTestV1 = createEchoTestV1();
        String response = echoTestV1.echo(ECHO_MESSAGE);
        assertEquals(ECHO_MESSAGE, response);
    }

    @Test
    void echoPingThrowExceptionTest() {
        IntegrationException e = assertThrows(IntegrationException.class, () -> {
            EchoTestV1 echoTestV1 = createEchoTestV1();
            echoTestV1.ping(true);
        });

        assertEquals("generic", e.getCode());
        assertEquals("Integration exception has been requested in parameters", e.getMessage());
    }

    @Test
    void echoSendThrowExceptionTest() {
        IntegrationException e = assertThrows(IntegrationException.class, () -> {
            EchoTestV1 echoTestV1 = createEchoTestV1();
            echoTestV1.echo(ECHO_MESSAGE, true);
        });

        assertEquals("generic", e.getCode());
        assertEquals("Integration exception has been requested in parameters", e.getMessage());
    }

    @Test
    void echoAsyncPingTest() throws Exception {
        EchoTestV1 echoTestV1 = createEchoTestV1();
        AwaitVoidCallback callback = new AwaitVoidCallback();
        echoTestV1.asyncPing(callback);
        Object result = awaitResultFromQueue(callback.getQueue());

        assertFalse(result instanceof IntegrationException, "Got integration error: " + result);
    }

    @Test
    void echoAsyncSendTest() throws Exception {
        EchoTestV1 echoTestV1 = createEchoTestV1();
        AwaitCallback callback = new AwaitCallback();
        echoTestV1.asyncEcho(ECHO_MESSAGE, callback);
        Object result = awaitResultFromQueue(callback.getQueue());

        assertFalse(result instanceof IntegrationException, "Got integration error: " + result);
        assertEquals(ECHO_MESSAGE, result);
    }

    @Test
    void echoAsyncPingThrowExceptionTest() throws Exception {
        EchoTestV1 echoTestV1 = createEchoTestV1();
        AwaitVoidCallback callback = new AwaitVoidCallback();
        echoTestV1.asyncPing(true, callback);
        Object result = awaitResultFromQueue(callback.getQueue());

        assertTrue(result instanceof IntegrationException, "Expected integration error");

        IntegrationException e = (IntegrationException) result;
        assertEquals("generic", e.getCode());
        assertEquals("Integration exception has been requested in parameters", e.getMessage());
    }

    @Test
    void echoAsyncSendThrowExceptionTest() throws Exception {
        EchoTestV1 echoTestV1 = createEchoTestV1();
        AwaitCallback callback = new AwaitCallback();
        echoTestV1.asyncEcho(ECHO_MESSAGE, true, callback);
        Object result = awaitResultFromQueue(callback.getQueue());

        assertTrue(result instanceof IntegrationException, "Expected integration error");

        IntegrationException e = (IntegrationException) result;
        assertEquals("generic", e.getCode());
        assertEquals("Integration exception has been requested in parameters", e.getMessage());
    }

    private EchoTestV1 createEchoTestV1() {
        Connection connection = createEchoDefaultConnection();
        return new EchoTestV1(connection);
    }

    private Connection createEchoDefaultConnection() {
        Connector connector = electrica.connector(EchoTestV1.ERN);
        return connector.defaultConnection();
    }

    @Test
    void submitMessageTest() throws Exception {
        Connection connection = createEchoDefaultConnection();

        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        UUID uuid = connection.addMessageListener(p -> true, message -> {
            queue.add(message);
            return Optional.empty();
        });

        postWebhookMessage("/submit");

        Message message = awaitResultFromQueue(queue);
        assertMessage(connection, message, false);

        connection.removeMessageListener(uuid);
    }

    @Test
    void invokeMessageReturnObjectTest() throws Exception {
        Connection connection = createEchoDefaultConnection();

        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        UUID uuid = connection.addMessageListener(p -> true, message -> {
            queue.add(message);
            return Optional.of(WebhookMessage.INSTANCE);
        });

        Response response = postWebhookMessage("/invoke");

        Message message = awaitResultFromQueue(queue);
        assertMessage(connection, message, true);

        WebhookMessage result = gson.fromJson(response.body().string(), WebhookMessage.class);
        assertEquals(WebhookMessage.INSTANCE, result);

        connection.removeMessageListener(uuid);
    }

    @Test
    void invokeMessageReturnStringTest() throws Exception {
        Connection connection = createEchoDefaultConnection();
        String expectedResult = "Test string return result";

        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        UUID uuid = connection.addMessageListener(p -> true, message -> {
            queue.add(message);
            return Optional.of(expectedResult);
        });

        Response response = postWebhookMessage("/invoke");

        Message message = awaitResultFromQueue(queue);
        assertMessage(connection, message, true);

        // TODO Fix additional string wrapping on backend !!!!
        assertEquals('"' + expectedResult + '"', response.body().string());

        connection.removeMessageListener(uuid);
    }

    @Test
    void invokeMessageReturnIntegerTest() throws Exception {
        Connection connection = createEchoDefaultConnection();
        int expectedResult = 23874624;

        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        UUID uuid = connection.addMessageListener(p -> true, message -> {
            queue.add(message);
            return Optional.of(expectedResult);
        });

        Response response = postWebhookMessage("/invoke");

        Message message = awaitResultFromQueue(queue);
        assertMessage(connection, message, true);

        assertEquals(expectedResult, Integer.parseInt(response.body().string()));

        connection.removeMessageListener(uuid);
    }

    @Test
    void invokeMessageReturnBooleanTest() throws Exception {
        Connection connection = createEchoDefaultConnection();
        boolean expectedResult = true;

        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        UUID uuid = connection.addMessageListener(p -> true, message -> {
            queue.add(message);
            return Optional.of(expectedResult);
        });

        Response response = postWebhookMessage("/invoke");

        Message message = awaitResultFromQueue(queue);
        assertMessage(connection, message, true);

        assertEquals(expectedResult, Boolean.parseBoolean(response.body().string()));

        connection.removeMessageListener(uuid);
    }

    @Test
    void invokeMessageReturnDoubleTest() throws Exception {
        Connection connection = createEchoDefaultConnection();
        double expectedResult = 3.14;

        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        UUID uuid = connection.addMessageListener(p -> true, message -> {
            queue.add(message);
            return Optional.of(expectedResult);
        });

        Response response = postWebhookMessage("/invoke");

        Message message = awaitResultFromQueue(queue);
        assertMessage(connection, message, true);

        assertEquals(expectedResult, Double.parseDouble(response.body().string()));

        connection.removeMessageListener(uuid);
    }

    @Test
    void invokeMessageReturnNothingTest() throws Exception {
        Connection connection = createEchoDefaultConnection();

        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        UUID uuid = connection.addMessageListener(p -> true, message -> {
            queue.add(message);
            return Optional.empty();
        });

        Response response = postWebhookMessage("/invoke");

        Message message = awaitResultFromQueue(queue);
        assertMessage(connection, message, true);

        assertEquals("null", response.body().string());

        connection.removeMessageListener(uuid);
    }

    private Response postWebhookMessage(String actionSuffix) throws IOException {
        Request request = new Request.Builder()
                .url(WEBHOOK_URL + actionSuffix)
                .header("Authorization", "Bearer " + ACCESS_KEY)
                .post(RequestBody.create(MEDIA_TYPE, gson.toJson(WebhookMessage.INSTANCE)))
                .build();
        Response response = httpClient.newCall(request).execute();
        assertTrue(response.isSuccessful(), "Webhook message bad response: " + response);
        return response;
    }

    @Getter
    private static class AwaitVoidCallback implements Callback<Void> {

        private final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);

        @Override
        public void onResponse(Void result) {
            queue.add(new Object());
        }

        @Override
        public void onFailure(IntegrationException exception) {
            queue.add(exception);
        }
    }

    @Getter
    private static class AwaitCallback implements Callback<String> {

        private final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);

        @Override
        public void onResponse(String result) {
            queue.add(result);
        }

        @Override
        public void onFailure(IntegrationException exception) {
            queue.add(exception);
        }
    }

    @Getter
    @Setter
    @Builder
    @EqualsAndHashCode
    private static class WebhookMessage {

        private static final WebhookMessage INSTANCE = WebhookMessage.builder()
                .stringField("Test string text")
                .integerField(128736123)
                .booleanField(true)
                .build();

        private String stringField;
        private Integer integerField;
        private Boolean booleanField;
    }
}
