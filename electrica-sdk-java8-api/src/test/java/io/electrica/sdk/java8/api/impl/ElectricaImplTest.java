package io.electrica.sdk.java8.api.impl;

import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.http.HttpModule;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ElectricaImplTest {

    @Test
    void testCreateConnector() throws Exception {
        String accessKey = "test_access_key";
        HttpModule httpModule = mock(HttpModule.class);

        Electrica electricaFef;
        Connector connectorRef;
        try (Electrica electrica = Electrica.instance(httpModule, accessKey)) {
            electricaFef = electrica;

            assertNotNull(electrica.getInstanceId());
            assertEquals(accessKey, electrica.getAccessKey());
            assertSame(httpModule, electrica.getHttpModule());
            assertFalse(electrica.isClosed());

            assertEquals(0, electrica.getConnectors().size());
            Connector connector = electrica.connector("test_ern");
            connectorRef = connector;
            assertFalse(connector.isClosed());

            assertEquals(1, electrica.getConnectors().size());
        }

        assertTrue(electricaFef.isClosed());
        assertTrue(connectorRef.isClosed());

        verify(httpModule, atLeastOnce()).close(eq(electricaFef.getInstanceId()));
    }

    @Test
    void testCreateConnectorAfterCloseError() throws Exception {
        String accessKey = "test_access_key";
        HttpModule httpModule = mock(HttpModule.class);

        Electrica electricaFef;
        try (Electrica electrica = Electrica.instance(httpModule, accessKey)) {
            electricaFef = electrica;
            electrica.connector("test_ern");
        }

        assertThrows(IllegalStateException.class, () ->
                electricaFef.connector("test_ern")
        );
    }

    @Test
    void testMessageListeners() throws Exception {
        String accessKey = "test_access_key";
        HttpModule httpModule = mock(HttpModule.class);

        UUID uuid;
        Electrica electricaFef;
        try (Electrica electrica = Electrica.instance(httpModule, accessKey)) {
            electricaFef = electrica;

            uuid = electrica.addMessageListener(Predicate.isEqual(null), message -> null);
            electrica.removeMessageListener(uuid);
        }

        verify(httpModule, atLeastOnce()).addMessageListener(
                eq(electricaFef.getInstanceId()),
                any(Predicate.class),
                any(Consumer.class)
        );

        verify(httpModule, atLeastOnce()).removeMessageListener(eq(electricaFef.getInstanceId()), eq(uuid));
    }

}
