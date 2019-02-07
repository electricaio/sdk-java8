package io.electrica.sdk.java8.slack.channel.v1;

import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.exception.ConnectionNotFoundException;

import javax.annotation.concurrent.ThreadSafe;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@ThreadSafe
public class SlackChannelV1Manager implements AutoCloseable {

    public static final String ERN = "ern://slack:channel:1";
    public static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(60);

    private final Connector connector;
    private final boolean exclusive;
    private final long timeout;

    private Map<String, SlackChannelV1> channelsByName;
    private Map<String, SlackChannelV1> channelsByConnectionName;

    public SlackChannelV1Manager(Electrica electrica) {
        this(electrica, false, DEFAULT_TIMEOUT);
    }

    public SlackChannelV1Manager(Electrica electrica, boolean exclusive, long timeout) {
        this(connector(electrica), exclusive, timeout);
    }

    public SlackChannelV1Manager(Connector connector) {
        this(connector, false, DEFAULT_TIMEOUT);
    }

    public SlackChannelV1Manager(Connector connector, boolean exclusive, long timeout) {
        this.connector = connector;
        this.exclusive = exclusive;
        this.timeout = timeout;
    }

    public static Connector connector(Electrica electrica) {
        return electrica.connector(ERN);
    }

    public Connector getConnector() {
        return connector;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public long getTimeout() {
        return timeout;
    }

    public synchronized void init() {
        if (channelsByName == null) {
            List<Connection> connections = connector.allConnections();
            channelsByName = connections.stream()
                    .collect(Collectors.toMap(
                            connection -> connection.getStringRequired(SlackChannelV1.CHANNEL_NAME_PROPERTY_KEY),
                            connection -> new SlackChannelV1(connection, timeout)
                    ));
            channelsByConnectionName = connections.stream()
                    .collect(Collectors.toMap(
                            Connection::getName,
                            connection -> new SlackChannelV1(connection, timeout)
                    ));
        }
    }

    public Map<String, SlackChannelV1> getChannelsByName() {
        init();
        return Collections.unmodifiableMap(channelsByName);
    }

    public Map<String, SlackChannelV1> getChannelsByConnectionName() {
        init();
        return Collections.unmodifiableMap(channelsByConnectionName);
    }

    public SlackChannelV1 getChannelByName(String channelName) {
        init();
        SlackChannelV1 slackChannel = channelsByName.get(channelName);
        if (slackChannel == null) {
            throw new ConnectionNotFoundException("Connection not found by Channel name: " + channelName);
        }
        return slackChannel;
    }

    public SlackChannelV1 getChannelByConnectionName(String connectionName) {
        init();
        SlackChannelV1 slackChannel = channelsByConnectionName.get(connectionName);
        if (slackChannel == null) {
            throw new ConnectionNotFoundException("Connection not found by Connection name: " + connectionName);
        }
        return slackChannel;
    }

    public Set<String> getChannelNames() {
        init();
        return new HashSet<>(channelsByName.keySet());
    }

    public Set<String> getConnectionNames() {
        init();
        return new HashSet<>(channelsByConnectionName.keySet());
    }

    public List<SlackChannelV1> getChannels() {
        init();
        return new ArrayList<>(channelsByConnectionName.values());
    }

    @Override
    public synchronized void close() throws Exception {
        if (exclusive) {
            connector.getElectrica().close();
        } else {
            connector.close();
        }

        channelsByName = null;
        channelsByConnectionName = null;
    }

}
