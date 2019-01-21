package io.electrica.sdk.java8.api.http;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

public interface Message {

    UUID getId();

    UUID getWebhookId();

    UUID getWebhookServiceId();

    String getName();

    Long getOrganizationId();

    Long getUserId();

    Long getAccessKeyId();

    boolean isPublic();

    Scope getScope();

    /**
     * Additional data for {@link Scope#Connector} scope.
     */
    @Nullable
    Long getConnectorId();

    /**
     * Additional data for {@link Scope#Connector} scope.
     */
    @Nullable
    String getConnectorErn();

    /**
     * Additional data for {@link Scope#Connection} scope.
     */
    @Nullable
    Long getConnectionId();

    Map<String, String> getPropertiesMap();

    Boolean getExpectedResult();

    /**
     * MediaType according to 'Content-Type' header.
     */
    String getContentType();

    /**
     * MediaType according to 'Accept' header. Null if expected result is false.
     */
    String getExpectedContentType();

    /**
     * Body of webhook request.
     */
    String getPayload();

    enum Scope {
        Connector, Connection, Custom
    }

}
