package io.electrica.sdk.java8.api.converter;

import io.electrica.sdk.java8.api.http.Message;

import java.io.IOException;

@FunctionalInterface
public interface MessageToObjectConverter<T> {
    T parse(Message message) throws IOException;
}
