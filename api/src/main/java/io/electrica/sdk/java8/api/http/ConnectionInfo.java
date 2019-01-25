package io.electrica.sdk.java8.api.http;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Result from Electrica service after verifying and getting connection data.
 */
public interface ConnectionInfo {

    Long getId();

    String getName();

    @Nullable
    Map<String, String> getProperties();

}
