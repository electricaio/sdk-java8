package io.electrica.sdk.java8.api.impl;

import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.http.ConnectionInfo;
import io.electrica.sdk.java8.api.http.HttpModule;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class ConnectorImplTest {

    @Test
    void testCreateConnection() throws Exception {
        String accessKey = "test_access_key";
        String ern = "test_ern";
        HttpModule httpModule = mock(HttpModule.class);

        Electrica electrica = Electrica.instance(httpModule, accessKey);

        Long connectionId = 100L;
        String connectionName = Connection.DEFAULT_NAME;
        Map<String, String> connectionProperties = new HashMap<>();

        ConnectionInfo info = mock(ConnectionInfo.class);
        when(info.getId()).thenReturn(connectionId);
        when(info.getName()).thenReturn(connectionName);
        when(info.getProperties()).thenReturn(connectionProperties);

        List<ConnectionInfo> connectionInfos = Collections.singletonList(info);
        doReturn(connectionInfos)
                .when(httpModule)
                .getConnections(eq(electrica.getInstanceId()), eq(connectionName), eq(ern));
        doReturn(connectionInfos)
                .when(httpModule)
                .getConnections(eq(electrica.getInstanceId()), isNull(), eq(ern));

        Connector connectorRef;
        Connection connectionRef;
        try (Connector connector = new ConnectorImpl(electrica, ern)) {
            connectorRef = connector;
            assertEquals(ern, connector.getErn());
            assertSame(electrica, connector.getElectrica());

            assertEquals(0, connector.getConnections().size());
            Connection connection = connector.defaultConnection();
            connectionRef = connection;
            assertEquals(1, connector.getConnections().size());

            List<Connection> allConnections = connector.allConnections();
            assertEquals(1, allConnections.size());
            assertEquals(connection, allConnections.get(0));

            assertEquals(1, connector.getConnections().size());

            assertEquals(connectionId, connection.getId());
            assertEquals(connectionName, connection.getName());
            assertSame(connector, connection.getConnector());

            assertFalse(connector.isClosed());
            assertFalse(connection.isClosed());
        }

        assertTrue(connectorRef.isClosed());
        assertTrue(connectionRef.isClosed());
    }

    @Test
    void testCreateConnectionAfterCloseError() throws Exception {
        String accessKey = "test_access_key";
        String ern = "test_ern";
        HttpModule httpModule = mock(HttpModule.class);

        Electrica electrica = Electrica.instance(httpModule, accessKey);

        Connector connectorRef;
        try (Connector connector = new ConnectorImpl(electrica, ern)) {
            connectorRef = connector;
        }

        assertThrows(IllegalStateException.class, connectorRef::defaultConnection);
    }

    @Test
    void testMessageListeners() throws Exception {
        String accessKey = "test_access_key";
        String ern = "test_ern";
        HttpModule httpModule = mock(HttpModule.class);

        Electrica electrica = Electrica.instance(httpModule, accessKey);

        UUID uuid;
        try (Connector connector = new ConnectorImpl(electrica, ern)) {
            uuid = connector.addMessageListener(Predicate.isEqual(null), message -> Optional.empty());
            connector.removeMessageListener(uuid);
        }

        verify(httpModule, atLeastOnce()).addMessageListener(
                eq(electrica.getInstanceId()),
                any(Predicate.class),
                any(Consumer.class)
        );

        verify(httpModule, atLeastOnce()).removeMessageListener(eq(electrica.getInstanceId()), eq(uuid));
    }
}
