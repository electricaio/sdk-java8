package io.electrica.sdk.java8.api.http;

import java.util.Map;

/**
 * Result from Electrica service after verifying and getting connection data.
 */
public interface ConnectionInfo {

    Long getId();

    String getName();

    Map<String, String> getProperties();

}
