package io.electrica.sdk.java8.core;

import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.http.HttpModule;

public class TestUtils {

    private static final String API_URL_ENV = "ELECTRICA_SDK_JAVA8_API_URL";
    private static final String DEFAULT_API_URL = "https://api.stage.electrica.io";

    private static final String ACCESS_KEY_ENV = "ELECTRICA_SDK_JAVA8_ACCESS_KEY";
    private static final String DEFAULT_ACCESS_KEY = "" +
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOlsiYyIsInUiLCJjaCIsInciLCJpIiwid3MiXSwidXNlcl9uYW1lIjo" +
            "iQGlkOjMiLCJzY29wZSI6WyJyIiwic2RrIl0sImV4cCI6MzY5NTc0Nzc0OCwiaWF0IjoxNTQ4MjY0MTAxLCJhdXRob3JpdGllcyI" +
            "6WyJrZXk6MSIsIm9yZzoxIl0sImp0aSI6ImRiMTUzNTEyLTM1NTMtNGUzMC05NTM5LTU3MGFhNjNkMWU1NiIsImNsaWVudF9pZCI" +
            "6ImFjY2Vzc0tleSJ9.KmZQDqSe8RQ9iFyQtC9LqPeFv8fy8N5VeN1-Pz7VL0Dcl1kyN5GwdSYxIDQWGfS2go08cbGofd3K4Y2Y_A" +
            "Pn859WM4alYaaRCPtgrM9rJ7b6Y0YfgVEpNUrer7hlXUhmoy1DjgsFMwnImYuF0fH8RoR6YrJyts49BNcJbaJnWs-lsU19ogMfGa" +
            "r-hAaoX_0dFbRK66SCGvq94NiL54ohHlLpdgmqlIKSaDRUKkYWRImTfhjoI3mlPL3gQ2U7eiwU8RrV8Zl3vUqcaGArEaDX1Etycj" +
            "qfsojKscUys7Fm8M3NdJdwe7KJ2GnByr3eF_inmehi41YVGNjKfSkgBBHsbg";

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
