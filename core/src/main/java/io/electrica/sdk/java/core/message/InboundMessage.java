package io.electrica.sdk.java.core.message;

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class InboundMessage {

    public static final TypeAdapterFactory TYPE_ADAPTER_FACTORY = new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (type.getRawType() != InboundMessage.class) {
                return null;
            }

            return new TypeAdapter<T>() {
                @Override
                public void write(JsonWriter jsonWriter, final T t) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public T read(final JsonReader jsonReader) throws JsonParseException {
                    JsonElement tree = Streams.parse(jsonReader);
                    JsonObject object = tree.getAsJsonObject();
                    JsonElement typeElement = object.get("@type");
                    if (typeElement == null) {
                        throw new JsonParseException("Required @type discriminator");
                    }
                    String type = typeElement.getAsString();
                    switch (type) {
                        case "webhook":
                            //noinspection unchecked
                            return (T) gson.getDelegateAdapter(
                                    TYPE_ADAPTER_FACTORY,
                                    TypeToken.get(WebhookInboundMessage.class)
                            ).fromJsonTree(tree);
                        default:
                            throw new JsonParseException("Unsupported message type: " + type);
                    }
                }
            };
        }
    };

    private UUID id;

}
