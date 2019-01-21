package io.electrica.sdk.java8.hackerrank.tests.v1;

import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;

import javax.annotation.concurrent.ThreadSafe;
import java.util.concurrent.TimeUnit;

@ThreadSafe
public class HackerRankTestsV1Manager implements AutoCloseable {

    public static final String ERN = "ern://hackerrank-v3:tests:1";

    public static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(60);

    private final Connector connector;
    private final boolean exclusive;
    private final long timeout;


    public HackerRankTestsV1Manager(Electrica electrica) {
        this(electrica, false, DEFAULT_TIMEOUT);
    }

    public HackerRankTestsV1Manager(Electrica electrica, boolean exclusive, long timeout) {
        this(connector(electrica), exclusive, timeout);
    }

    public HackerRankTestsV1Manager(Connector connector) {
        this(connector, false, DEFAULT_TIMEOUT);
    }

    public HackerRankTestsV1Manager(Connector connector, boolean exclusive, long timeout) {
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

    public HackerRankTestsV1 getConnectionByName(String connectionName) {
       Connection hackerRankConnection = connector.allConnections().stream()
               .filter(connection -> connectionName.equals(connection.getName()))
               .findAny()
               .orElse(null);

       return new HackerRankTestsV1(hackerRankConnection);
    }

    @Override
    public synchronized void close() throws Exception {
        if (exclusive) {
            connector.getElectrica().close();
        } else {
            connector.close();
        }
    }

}
