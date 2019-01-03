package io.electrica.sdk.java8.api.impl;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.api.http.ConnectionInfo;
import io.electrica.sdk.java8.api.http.HttpModule;
import io.electrica.sdk.java8.api.http.Request;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConnectionImplTest {

    @Test
    void testConnectionProperties() throws Exception {
        String accessKey = "test_access_key";
        HttpModule httpModule = mock(HttpModule.class);
        Electrica electrica = Electrica.instance(httpModule, accessKey);

        Long connectionId = 100L;
        String connectionName = ConnectorImpl.DEFAULT_CONNECTION;
        Map<String, String> connectionProperties = new HashMap<>();
        connectionProperties.put("number", "100");
        connectionProperties.put("boolean", "true");
        connectionProperties.put("double", "1.8");
        connectionProperties.put("string", "text");

        ConnectionInfo info = mock(ConnectionInfo.class);
        when(info.getId()).thenReturn(connectionId);
        when(info.getName()).thenReturn(connectionName);
        when(info.getProperties()).thenReturn(connectionProperties);

        Connector connector = mock(Connector.class);
        when(connector.getElectrica()).thenReturn(electrica);

        Connection connectionRef;
        try (Connection connection = new ConnectionImpl(connector, info)) {
            connectionRef = connection;

            assertEquals(connectionId, connection.getId());
            assertEquals(connectionName, connection.getName());
            assertSame(connector, connection.getConnector());

            assertEquals(true, connection.getBoolean("boolean"));
            assertEquals(true, connection.getBooleanRequired("boolean"));
            assertNull(connection.getBoolean("boolean-absent"));
            assertEquals(false, connection.getBoolean("boolean-absent", false));

            assertEquals((Integer) 100, connection.getInteger("number"));
            assertEquals(100, connection.getIntegerRequired("number"));
            assertNull(connection.getInteger("number-absent"));
            assertEquals((Integer) 101, connection.getInteger("number-absent", 101));

            assertEquals((Long) 100L, connection.getLong("number"));
            assertEquals(100, connection.getLongRequired("number"));
            assertNull(connection.getLong("number-absent"));
            assertEquals((Long) 101L, connection.getLong("number-absent", 101L));

            assertEquals((Double) 1.8, connection.getDouble("double"));
            assertEquals(1.8, connection.getDoubleRequired("double"), 0x1e - 6);
            assertNull(connection.getDouble("double-absent"));
            assertEquals((Double) 0.8, connection.getDouble("double-absent", 0.8));

            assertEquals("text", connection.getString("string"));
            assertEquals("text", connection.getStringRequired("string"));
            assertNull(connection.getString("string-absent"));
            assertEquals("another-text", connection.getString("string-absent", "another-text"));

            assertFalse(connection.isClosed());
        }

        assertTrue(connectionRef.isClosed());
    }

    @Test
    void testInvokeAfterCloseError() throws Exception {
        String accessKey = "test_access_key";
        HttpModule httpModule = mock(HttpModule.class);
        Electrica electrica = Electrica.instance(httpModule, accessKey);
        ConnectionInfo info = mock(ConnectionInfo.class);

        Connector connector = mock(Connector.class);
        when(connector.getElectrica()).thenReturn(electrica);

        Connection connectionRef;
        try (Connection connection = new ConnectionImpl(connector, info)) {
            connectionRef = connection;
        }

        assertThrows(IllegalStateException.class, () ->
                connectionRef.invoke("test", null, null, 1L, TimeUnit.SECONDS)
        );
    }

    @Test
    void testSyncInvoke() throws Exception {
        String accessKey = "test_access_key";
        HttpModule httpModule = mock(HttpModule.class);
        Electrica electrica = Electrica.instance(httpModule, accessKey);
        ConnectionInfo info = mock(ConnectionInfo.class);
        Connector connector = mock(Connector.class);
        when(connector.getElectrica()).thenReturn(electrica);

        doAnswer(invocation -> {
            Callback<Object> rh = invocation.getArgument(3);
            rh.onResponse(null);
            return null;
        })
                .when(httpModule)
                .submitJob(
                        eq(electrica.getInstanceId()),
                        any(Request.class),
                        eq(Void.class),
                        any(Callback.class)
                );

        try (Connection connection = new ConnectionImpl(connector, info)) {
            connection.invoke("test_action", null, null, 1L, TimeUnit.SECONDS);
        }

        verify(httpModule, atLeastOnce()).submitJob(
                eq(electrica.getInstanceId()),
                any(Request.class),
                eq(Void.class),
                any(Callback.class)
        );
    }

    @Test
    void testSyncInvokeWithIntegrationException() throws Exception {
        String accessKey = "test_access_key";
        HttpModule httpModule = mock(HttpModule.class);
        Electrica electrica = Electrica.instance(httpModule, accessKey);
        ConnectionInfo info = mock(ConnectionInfo.class);
        Connector connector = mock(Connector.class);
        when(connector.getElectrica()).thenReturn(electrica);

        doAnswer(invocation -> {
            Callback<Object> rh = invocation.getArgument(3);
            rh.onFailure(new IntegrationException("code", "message", "stackTrace", null));
            return null;
        })
                .when(httpModule)
                .submitJob(
                        eq(electrica.getInstanceId()),
                        any(Request.class),
                        eq(Void.class),
                        any(Callback.class)
                );

        try (Connection connection = new ConnectionImpl(connector, info)) {
            assertThrows(IntegrationException.class, () ->
                    connection.invoke("test_action", null, null, 1L, TimeUnit.SECONDS)
            );
        }
    }

    @Test
    void testSyncInvokeTimeoutException() throws Exception {
        String accessKey = "test_access_key";
        HttpModule httpModule = mock(HttpModule.class);
        Electrica electrica = Electrica.instance(httpModule, accessKey);
        ConnectionInfo info = mock(ConnectionInfo.class);
        Connector connector = mock(Connector.class);
        when(connector.getElectrica()).thenReturn(electrica);

        try (Connection connection = new ConnectionImpl(connector, info)) {
            assertThrows(TimeoutException.class, () ->
                    connection.invoke("test_action", null, null, 100L, TimeUnit.MILLISECONDS)
            );
        }
    }

    @Test
    void testMessageListeners() throws Exception {
        String accessKey = "test_access_key";
        HttpModule httpModule = mock(HttpModule.class);
        Electrica electrica = Electrica.instance(httpModule, accessKey);
        ConnectionInfo info = mock(ConnectionInfo.class);
        Connector connector = mock(Connector.class);
        when(connector.getElectrica()).thenReturn(electrica);

        UUID uuid;
        try (Connection connection = new ConnectionImpl(connector, info)) {
            uuid = connection.addMessageListener(Predicate.isEqual(null), message -> Optional.empty());
            connection.removeMessageListener(uuid);
        }

        verify(httpModule, atLeastOnce()).addMessageListener(
                eq(electrica.getInstanceId()),
                any(Predicate.class),
                any(Consumer.class)
        );

        verify(httpModule, atLeastOnce()).removeMessageListener(eq(electrica.getInstanceId()), eq(uuid));
    }

}
