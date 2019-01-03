package io.electrica.sdk.java8.slack.channel.v1;

import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.exception.ConnectionNotFoundException;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ThreadSafe
public class SlackChannelV1Manager implements AutoCloseable {

    public static final String ERN = "ern://slack:channel:1";

    private final Electrica electrica;
    private final boolean exclusive;

    private Connector connector;
    private Map<String, SlackChannelV1> channelsByName;
    private Map<String, SlackChannelV1> channelsByConnectionName;

    public SlackChannelV1Manager(Electrica electrica) {
        this(electrica, false);
    }

    public SlackChannelV1Manager(Electrica electrica, boolean exclusive) {
        this.electrica = electrica;
        this.exclusive = exclusive;
    }

    public synchronized void init() {
        if (connector == null) {
            connector = electrica.connector(ERN);
            List<Connection> connections = connector.allConnections();
            channelsByName = connections.stream()
                    .collect(Collectors.toMap(
                            connection -> connection.getStringRequired(SlackChannelV1.CHANNEL_NAME_PROPERTY_KEY),
                            SlackChannelV1::new
                    ));
            channelsByConnectionName = connections.stream()
                    .collect(Collectors.toMap(
                            Connection::getName,
                            SlackChannelV1::new
                    ));
        }
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

    public List<String> getChannelNames() {
        init();
        return new ArrayList<>(channelsByConnectionName.keySet());
    }

    public List<String> getConnectionNames() {
        init();
        return new ArrayList<>(channelsByName.keySet());
    }

    public List<SlackChannelV1> getChannels() {
        init();
        return new ArrayList<>(channelsByConnectionName.values());
    }

    @Override
    public synchronized void close() throws Exception {
        if (exclusive) {
            electrica.close();
        } else if (connector != null) {
            connector.close();
        }

        channelsByName = null;
        channelsByConnectionName = null;
    }

}
