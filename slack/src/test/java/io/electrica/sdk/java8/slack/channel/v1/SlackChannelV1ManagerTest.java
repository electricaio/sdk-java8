package io.electrica.sdk.java8.slack.channel.v1;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.core.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SlackChannelV1ManagerTest {

    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private static final String TEXT_MESSAGE = "Test message from `electrica/sdk-java8` repo: ";

    private static final String FIRST_CHANNEL_NAME = "first-channel";
    private static final String FIRST_CONNECTION_NAME = "First Channel";

    private static final String SECOND_CHANNEL_NAME = "second-channel";
    private static final String SECOND_CONNECTION_NAME = "Second Channel";

    private static final Set<String> ALL_CHANNEL_NAMES =
            new HashSet<>(Arrays.asList(FIRST_CHANNEL_NAME, SECOND_CHANNEL_NAME));
    private static final Set<String> ALL_CONNECTION_NAMES =
            new HashSet<>(Arrays.asList(FIRST_CONNECTION_NAME, SECOND_CONNECTION_NAME));

    private static Electrica electrica;
    private static SlackChannelV1Manager manager;

    @BeforeAll
    static void setUp() {
        electrica = TestUtils.createElectrica();
        manager = new SlackChannelV1Manager(electrica, true, TIMEOUT);
    }

    @AfterAll
    static void tearDown() throws Exception {
        Connector connector = manager.getConnector();

        assertFalse(connector.isClosed());
        assertFalse(electrica.isClosed());

        manager.close();

        assertTrue(electrica.isClosed(), "Exclusive manager have to close Electrica instance");
        assertTrue(connector.isClosed(), "Must be closed as part of Electrica instance");
    }

    private static void assertChannel(SlackChannelV1 channel, String name, String connectionName) {
        assertNotNull(channel);
        Connection connection = channel.getConnection();
        assertEquals(name, connection.getStringRequired(SlackChannelV1.CHANNEL_NAME_PROPERTY_KEY));
        assertEquals(connectionName, connection.getName());
        assertEquals(TIMEOUT, channel.getTimeout());
    }

    private static void assertFirstChannel(SlackChannelV1 channel) {
        assertChannel(channel, FIRST_CHANNEL_NAME, FIRST_CONNECTION_NAME);
    }

    private static void assertSecondChannel(SlackChannelV1 channel) {
        assertChannel(channel, SECOND_CHANNEL_NAME, SECOND_CONNECTION_NAME);
    }

    @Test
    void notExclusiveManagerTest() throws Exception {
        SlackChannelV1Manager m = new SlackChannelV1Manager(electrica);
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
    void fieldsTest() {
        assertTrue(manager.isExclusive());
        assertNotNull(manager.getConnector());
        assertEquals(TIMEOUT, manager.getTimeout());
    }

    @Test
    void getChannelsByNameTest() {
        Map<String, SlackChannelV1> channelsByName = manager.getChannelsByName();
        assertEquals(2, channelsByName.size());
        assertFirstChannel(channelsByName.get(FIRST_CHANNEL_NAME));
        assertSecondChannel(channelsByName.get(SECOND_CHANNEL_NAME));
    }

    @Test
    void getChannelsByConnectionNameTest() {
        Map<String, SlackChannelV1> channelsByConnectionName = manager.getChannelsByConnectionName();
        assertEquals(2, channelsByConnectionName.size());
        assertFirstChannel(channelsByConnectionName.get(FIRST_CONNECTION_NAME));
        assertSecondChannel(channelsByConnectionName.get(SECOND_CONNECTION_NAME));
    }

    @Test
    void getChannelByNameTest() {
        assertFirstChannel(manager.getChannelByName(FIRST_CHANNEL_NAME));
        assertSecondChannel(manager.getChannelByName(SECOND_CHANNEL_NAME));
    }

    @Test
    void getChannelByConnectionName() {
        assertFirstChannel(manager.getChannelByConnectionName(FIRST_CONNECTION_NAME));
        assertSecondChannel(manager.getChannelByConnectionName(SECOND_CONNECTION_NAME));
    }

    @Test
    void getChannelNamesTest() {
        assertEquals(ALL_CHANNEL_NAMES, manager.getChannelNames());
    }

    @Test
    void getConnectionNamesTest() {
        assertEquals(ALL_CONNECTION_NAMES, manager.getConnectionNames());
    }

    @Test
    void getChannelsTest() {
        List<SlackChannelV1> channels = manager.getChannels();
        assertEquals(2, channels.size());
        for (SlackChannelV1 channel : channels) {
            switch (channel.getConnection().getName()) {
                case FIRST_CONNECTION_NAME:
                    assertFirstChannel(channel);
                    break;
                case SECOND_CONNECTION_NAME:
                    assertSecondChannel(channel);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    @Test
    void sendTextTest() throws Exception {
        SlackChannelV1 channel = manager.getChannelByName(FIRST_CHANNEL_NAME);
        assertFirstChannel(channel);
        channel.send(TEXT_MESSAGE + "sync mode");
    }

    @Test
    void submitTextTest() throws Exception {
        SlackChannelV1 channel = manager.getChannelByName(FIRST_CHANNEL_NAME);
        assertFirstChannel(channel);

        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
        channel.submit(TEXT_MESSAGE + "async mode", new Callback<Void>() {
            @Override
            public void onResponse(Void result) {
                queue.add(new Object());
            }

            @Override
            public void onFailure(IntegrationException exception) {
                queue.add(exception);
            }
        });
        Object result = queue.poll(TIMEOUT, TimeUnit.MILLISECONDS);
        assertNotNull(result, "Response timeout");
        if (result instanceof IntegrationException) {
            throw (IntegrationException) result;
        }
    }
}
