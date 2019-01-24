package io.electrica.sdk.java8.api;

import io.electrica.sdk.java8.api.converter.MessageToObjectConverter;
import io.electrica.sdk.java8.api.converter.ResultToObjectConverter;
import io.electrica.sdk.java8.api.http.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @param <T> payload deserialization type
 * @param <R> result payload serialization type
 */
public abstract class ObjectMessageListener<T, R> implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(ObjectMessageListener.class);

    private final MessageToObjectConverter<T> parser;
    private final ResultToObjectConverter<R> converter;

    public ObjectMessageListener(MessageToObjectConverter<T> parser, ResultToObjectConverter<R> converter) {
        this.parser = parser;
        this.converter = converter;
    }

    @Override
    public String onMessage(Message message) {
        try {
            T entity = parser.parse(message);
            R result = onMessage(message, entity);
            return converter.convert(message, result);
        } catch (Exception e) {
            return onException(message, e);
        }
    }

    public String onException(Message message, Exception e) {
        return defaultOnException(message, e);
    }

    private static String defaultOnException(Message message, Exception e) {
        logger.error("Parsing error for messageId:{} webhookId:{} contentType:{} payload:{}",
                message.getId(), message.getWebhookId(), message.getContentType(), message.getPayload(), e);
        return null;
    }

    public abstract R onMessage(Message message, T payload);

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Builder() {}

        public <E> MessageBuilder<E> convertToObject(MessageToObjectConverter<E> parser) {
            Objects.requireNonNull(parser);
            return new MessageBuilder<>(parser);
        }
    }

    public static class MessageBuilder<E> {
        private final MessageToObjectConverter<E> parser;

        private MessageBuilder(MessageToObjectConverter<E> parser) {
            this.parser = parser;
        }

        public <R> ConvertibleMessageBuilder<E, R> onMessage(BiFunction<Message, E, R> onMessage) {
            Objects.requireNonNull(onMessage);
            return new ConvertibleMessageBuilder<>(parser, onMessage);
        }
    }

    public static class ConvertibleMessageBuilder<E, R> {
        private final MessageToObjectConverter<E> parser;
        private final BiFunction<Message, E, R> onMessage;
        private ResultToObjectConverter<R> converter = (message, r) -> Objects.toString(r);
        private BiFunction<Message, Exception, String> onException = ObjectMessageListener::defaultOnException;

        private ConvertibleMessageBuilder(MessageToObjectConverter<E> parser, BiFunction<Message, E, R> onMessage) {
            this.parser = parser;
            this.onMessage = onMessage;
        }

        public ConvertibleMessageBuilder<E, R> convertToString(ResultToObjectConverter<R> converter) {
            this.converter = Objects.requireNonNull(converter);
            return this;
        }

        public ConvertibleMessageBuilder<E, R> onException(BiFunction<Message, Exception, String> onException) {
            this.onException = Objects.requireNonNull(onException);
            return this;
        }

        public ObjectMessageListener<E, R> build() {
            return new ObjectMessageListener<E, R>(parser, converter) {
                @Override
                public R onMessage(Message message, E payload) {
                    return onMessage.apply(message, payload);
                }

                @Override
                public String onException(Message message, Exception e) {
                    return onException.apply(message, e);
                }
            };
        }
    }
}

