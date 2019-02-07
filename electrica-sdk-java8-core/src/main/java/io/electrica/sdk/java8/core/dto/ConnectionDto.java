package io.electrica.sdk.java8.core.dto;

import io.electrica.sdk.java8.api.http.ConnectionInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Result from Electrica service after verifying and getting connection data.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDto implements ConnectionInfo {

    private Long id;
    private String name;
    private Map<String, String> properties;

}
