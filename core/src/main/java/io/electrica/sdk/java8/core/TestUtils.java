package io.electrica.sdk.java8.core;

import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.http.HttpModule;

public class TestUtils {

    private static final String API_URL_ENV = "ELECTRICA_SDK_JAVA8_API_URL";
    private static final String DEFAULT_API_URL = "https://api.stage.electrica.io";

    private static final String ACCESS_KEY_ENV = "ELECTRICA_SDK_JAVA8_ACCESS_KEY";
    private static final String DEFAULT_ACCESS_KEY = "" +
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsiYyIsInUiLCJjaCIsInciLCJpIiwid3MiXSwidXNlcl9uYW1lIjoi" +
            "QGlkOjMiLCJzY29wZSI6WyJyIiwic2RrIl0sImV4cCI6MzY5NDkzMTk1OSwiaWF0IjoxNTQ3NDQ4MzEyLCJhdXRob3JpdGllcyI6W" +
            "yJrZXk6MiIsIm9yZzoxIl0sImp0aSI6ImYyOTVmYjQzLWJhNWEtNDVjZS05ZWYwLTgzZjRmOTg5ZWFhMyIsImNsaWVudF9pZCI6Im" +
            "FjY2Vzc0tleSJ9.M2skv6qfklWqk9WSkmRGL-tilm4IYkVNIy71C1w7_bdSvcfAx26bAF4gipvklenv7YbGXSIhc4UFN3bMh6lm8P" +
            "34ezcKxWBuDUR89ET3lQKo9fJjLQdJGbrub7ojAascaHU4GKum43OOqMG8xKdJHyIczf8RbVP7YbzJOLFwZAUMHNyIYBw-yC4uRNi" +
            "e3FDh7LygFODu3E2CFr9uEWpktCmBqlfYiUFxbtKywEtLaQThQo4eVkf7s4hOVlngh3rNNpWf1_ay2uODfCd5x-qv0NLGNVL2QsCe" +
            "8pwW1X8KmxEsUO2tHMEAVXhVBE3UiTp7y6lAJ0eX1SPu6sBUtrrBNQ";

    private TestUtils() {
    }

    public static String getApiUrl() {
        String result = System.getenv(API_URL_ENV);
        return result == null ? DEFAULT_API_URL : result;
    }

    public static String getAccessKey() {
        String result = System.getenv(ACCESS_KEY_ENV);
        return result == null ? DEFAULT_ACCESS_KEY : result;
    }

    public static SingleInstanceHttpModule createSingleInstanceHttpModule() {
        return new SingleInstanceHttpModule(getApiUrl());
    }

    public static Electrica createElectrica() {
        HttpModule httpModule = createSingleInstanceHttpModule();
        return Electrica.instance(httpModule, getAccessKey());
    }
}
