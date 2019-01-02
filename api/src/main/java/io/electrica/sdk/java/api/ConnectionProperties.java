package io.electrica.sdk.java.api;

import javax.annotation.Nullable;

public interface ConnectionProperties {

    boolean contains(String key);

    @Nullable
    String getString(String key);

    default String getString(String key, String defaultValue) {
        String value = getString(key);
        return value == null ? defaultValue : value;
    }

    default String getStringRequired(String key) throws IllegalArgumentException {
        return required(getString(key), "Required configuration parameter '%s'", key);
    }

    @Nullable
    default Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    default Boolean getBoolean(String key, Boolean defaultValue) {
        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    default boolean getBooleanRequired(String key) throws IllegalArgumentException {
        return required(getBoolean(key), "Required configuration parameter '%s'", key);
    }

    @Nullable
    default Integer getInteger(String key) throws NumberFormatException {
        return getInteger(key, null);
    }

    default Integer getInteger(String key, Integer defaultValue) throws NumberFormatException {
        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    default int getIntegerRequired(String key) throws IllegalArgumentException {
        return required(getInteger(key), "Required configuration parameter '%s'", key);
    }

    @Nullable
    default Long getLong(String key) throws NumberFormatException {
        return getLong(key, null);
    }

    default Long getLong(String key, Long defaultValue) throws NumberFormatException {
        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    default long getLongRequired(String key) throws IllegalArgumentException {
        return required(getLong(key), "Required configuration parameter '%s'", key);
    }

    @Nullable
    default Double getDouble(String key) throws NumberFormatException {
        return getDouble(key, null);
    }

    default Double getDouble(String key, Double defaultValue) throws NumberFormatException {
        String value = getString(key);
        if (value == null) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    default double getDoubleRequired(String key) throws IllegalArgumentException {
        return required(getDouble(key), "Required configuration parameter '%s'", key);
    }

    default <T> T required(T value, String message, Object arg) throws IllegalArgumentException {
        if (value == null) {
            throw new IllegalArgumentException(String.format(message, arg));
        }
        return value;
    }

}
