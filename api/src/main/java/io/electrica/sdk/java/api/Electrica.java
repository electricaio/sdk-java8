package io.electrica.sdk.java.api;

import io.electrica.sdk.java.api.http.HttpModule;
import io.electrica.sdk.java.api.http.Message;
import io.electrica.sdk.java.api.impl.ElectricaImpl;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Represents Electrica object that handles connectors
 * for a single access key.
 * <p>
 * Example:
 * <p>
 * String accessKey = "here_goes_access_key";
 * Electrica e = Electrica.instance(accessKey);
 */
public interface Electrica extends AutoCloseable {

    /**
     * Creates Electrica instance.
     * From here, we can create connectors to different sources such as:
     * <p>
     * Greenhouse, SalesForce, MySQL etc
     * by using {@link Electrica#connector(String)}
     *
     * @return - electrica instance
     */
    static Electrica instance(HttpModule httpModule, String accessKey) {
        return new ElectricaImpl(httpModule, accessKey);
    }

    /**
     * Given the ern,
     * it creates a connector object to desired source.
     * <p>
     * Example:
     * <p>
     * Electrica e = Electrica.instance("here_goes_access_key");
     * Connector c = e.connector("ern://greenhouse:applications:1.0");
     */
    Connector connector(String ern);

    Set<Connector> getConnectors();

    HttpModule getHttpModule();

    String getAccessKey();

    UUID getInstanceId();

    UUID addMessageListener(Predicate<Message> filter, MessageListener listener);

    void removeMessageListener(UUID listenerId);

    boolean isClosed();

}
