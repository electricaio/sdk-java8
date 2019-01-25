package io.electrica.sdk.java8.brassring.application.v1;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.api.http.ConnectionInfo;
import io.electrica.sdk.java8.api.http.HttpModule;
import io.electrica.sdk.java8.api.http.Request;
import io.electrica.sdk.java8.brassring.application.v1.model.BrassRingApplicationPayload;
import io.electrica.sdk.java8.brassring.application.v1.model.FormInput;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BrassRingApplicationTest {

    private static Electrica electrica;
    private static BrassRingApplication brassRingApplication;

    @BeforeAll
    static void setUp() throws IOException {
        HttpModule httpModule = mock(HttpModule.class);
        electrica = Electrica.instance(httpModule, "key");
        mockHttpModule(httpModule);
    }

    @AfterAll
    static void tearDown() throws Exception {
        electrica.close();
    }

    private static void mockHttpModule(HttpModule httpModule) throws IOException {
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
                        eq(BrassRingApplication.ERN)
                );

        doAnswer(invocation -> {
            Callback<?> rh = invocation.getArgument(3);
            rh.onResponse(null);
            return null;
        })
                .when(httpModule)
                .submitJob(
                        eq(electrica.getInstanceId()),
                        any(Request.class),
                        any(),
                        any(Callback.class)
                );
    }

    @BeforeEach
    void setUpBrassRing() {
        Connector connector = electrica.connector(BrassRingApplication.ERN);
        Connection connection = connector.defaultConnection();
        brassRingApplication = new BrassRingApplication(connection);
    }

    @AfterEach
    void tearDownBrassRing() throws Exception {
        brassRingApplication.close();
        assertTrue(brassRingApplication.getConnection().isClosed());
    }

    @Test
    void testSyncUpdate() throws Exception {
        brassRingApplication.update(
                new BrassRingApplicationPayload()
                        .formTypeId(12345)
                        .addFieldsItem(new FormInput().id(1).name("FirstName").value("Doh"))
                        .addFieldsItem(new FormInput().id(2).name("LastName").value("Joe")),
                10,
                TimeUnit.SECONDS
        );
    }

    @Test
    void testASyncUpdate() throws Exception {
        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
        brassRingApplication.update(
                new BrassRingApplicationPayload()
                        .formTypeId(12345)
                        .addFieldsItem(new FormInput().id(1).name("FirstName").value("Doh"))
                        .addFieldsItem(new FormInput().id(2).name("LastName").value("Joe")),
                new Callback<Void>() {
                    @Override
                    public void onResponse(Void result) {
                        queue.add(new Object());
                    }

                    @Override
                    public void onFailure(IntegrationException exception) {
                        queue.add(exception);
                    }
                }
        );
        Object result = queue.poll(10, TimeUnit.SECONDS);
        if (result == null) {
            fail("Response timeout");
        } else if (result instanceof IntegrationException) {
            throw (IntegrationException) result;
        }
    }

}
