package io.electrica.sdk.java8.core.message;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.electrica.sdk.java8.api.ObjectMessageListener;
import io.electrica.sdk.java8.api.http.Message;

import java.util.function.BiFunction;

public class GsonObjectMessageListenerFactory {
    private final Gson gson;

    public GsonObjectMessageListenerFactory(Gson gson) {
        this.gson = gson;
    }

    public <E, F> ObjectMessageListener<E, F> create(Class<E> entityClass, Class<F> resultClass,
                                                     BiFunction<Message, E, F> onMessageFunction) {

        return ObjectMessageListener.newBuilder()
                .convertToObject(message -> gson.fromJson(message.getPayload(), entityClass))
                .onMessage(onMessageFunction)
                .convertToString((message, object) -> gson.toJson(object, resultClass))
                .build();
    }

    public <E> ObjectMessageListener<E, String> create(Class<E> entityClass,
                                                       BiFunction<Message, E, String> onMessageFunction) {
        return ObjectMessageListener.newBuilder()
                .convertToObject(message -> gson.fromJson(message.getPayload(), entityClass))
                .onMessage(onMessageFunction)
                .build();
    }

    public ObjectMessageListener<JsonElement, JsonElement> jsonToJson(
            BiFunction<Message, JsonElement, JsonElement> onMessageFunction) {
        return create(JsonElement.class, JsonElement.class, onMessageFunction);
    }

    public ObjectMessageListener<JsonElement, String> jsonToString(
            BiFunction<Message, JsonElement, String> onMessageFunction) {
        return create(JsonElement.class, onMessageFunction);
    }
}
