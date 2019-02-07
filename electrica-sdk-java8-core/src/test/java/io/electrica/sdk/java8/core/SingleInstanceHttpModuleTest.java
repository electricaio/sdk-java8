package io.electrica.sdk.java8.core;

import com.google.gson.Gson;
import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.*;
import io.electrica.sdk.java8.api.exception.ConnectionNotFoundException;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.api.http.Message;
import io.electrica.sdk.java8.echo.test.v1.EchoTestV1;
import lombok.*;
import okhttp3.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class SingleInstanceHttpModuleTest {

    private static final UUID WEBHOOK_ID = UUID.fromString("3afc6d4d-a0bd-4434-8eba-e982c5d7fb4f");
    private static final String WEBHOOK_URL = TestUtils.getApiUrl() + "/v1/webhooks/" + WEBHOOK_ID;
    private static final String WEBHOOK_NAME = "Default";
    private static final Map<String, String> WEBHOOK_PROPERTIES =
            Collections.singletonMap("some.test.property", "testValue");

    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");
    private static final String ECHO_MESSAGE = "Test echo message";
    private static final String CONNECTION_NOT_FOUND_MESSAGE = "Connection not found by name: ";
    private static final  Gson gson = new Gson();

    private static Electrica electrica;
    private static OkHttpClient httpClient;


    @BeforeAll
    static void setUp() {
        httpClient = new OkHttpClient.Builder().build();
        electrica = TestUtils.createElectrica();
    }

    @AfterAll
    static void tearDown() throws Exception {
        httpClient.dispatcher().cancelAll();
        electrica.close();
    }

    private static <T> T awaitResultFromQueue(BlockingQueue<T> queue) {
        try {
            T result = queue.poll(30, TimeUnit.SECONDS);
            assertNotNull(result, "Waiting of result timed out");
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
        assertEquals(WEBHOOK_PROPERTIES, message.getPropertiesMap());
        assertEquals(isExpectedResult, message.getExpectedResult());
        assertEquals(WebhookMessage.INSTANCE, gson.fromJson(message.getPayload(), WebhookMessage.class));
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

        UUIDMessageSupplier uuidMessageSupplier = addListener(connection, __ -> true, __ -> null);

        postWebhookMessage("/submit");

        Message message = uuidMessageSupplier.getMessage();
        assertMessage(connection, message, false);

        connection.removeMessageListener(uuidMessageSupplier.getUuid());
    }

    @Test
    void invokeMessageReturnStringTest() throws Exception {
        Connection connection = createEchoDefaultConnection();
        String expectedResult = "Test string return result";
        UUIDMessageSupplier uuidSupplierPair = addListener(connection, p -> true, __ -> expectedResult);

        Response response = postWebhookMessage("/invoke");

        Message message = uuidSupplierPair.getMessage();
        assertMessage(connection, message, true);

        assertEquals(expectedResult, response.body().string());

        connection.removeMessageListener(uuidSupplierPair.getUuid());
    }

    @Test
    void invokeMessageReturnNothingTest() throws Exception {
        Connection connection = createEchoDefaultConnection();
        UUIDMessageSupplier uuidMessageSupplier = addListener(connection, __ -> true, __ -> null);

        Response response = postWebhookMessage("/invoke");

        Message message = uuidMessageSupplier.getMessage();
        assertMessage(connection, message, true);

        String strResponse = response.body().string();
        assertTrue(strResponse == null || strResponse.isEmpty());

        connection.removeMessageListener(uuidMessageSupplier.getUuid());
    }

    private static UUIDMessageSupplier addListener(Connection connection, Predicate<Message> filter,
                                                      MessageListener listener) {
        BlockingQueue<Message> queue = new ArrayBlockingQueue<>(1);
        UUID uuid = connection.addMessageListener(filter, message -> {
            queue.add(message);
            return listener.onMessage(message);
        });
        return new UUIDMessageSupplier(uuid, () -> awaitResultFromQueue(queue));
    }

    private Response postWebhookMessage(String actionSuffix) throws IOException {
        Request request = new Request.Builder()
                .url(WEBHOOK_URL + actionSuffix)
                .header("Authorization", "Bearer " + TestUtils.getAccessKey())
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

    @Getter
    @AllArgsConstructor
    private static class UUIDMessageSupplier {
        private UUID uuid;
        @Getter(AccessLevel.NONE)
        private Supplier<Message> messageSupplier;

        public Message getMessage() {
            return messageSupplier.get();
        }
    }
}
