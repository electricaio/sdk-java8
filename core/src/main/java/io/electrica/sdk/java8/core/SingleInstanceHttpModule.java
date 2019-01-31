package io.electrica.sdk.java8.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.http.ConnectionInfo;
import io.electrica.sdk.java8.api.http.HttpModule;
import io.electrica.sdk.java8.api.http.Message;
import io.electrica.sdk.java8.api.http.Request;
import io.electrica.sdk.java8.core.dto.ConnectionDto;
import io.electrica.sdk.java8.core.dto.MessageImpl;
import io.electrica.sdk.java8.core.dto.MessageResultDto;
import io.electrica.sdk.java8.core.message.InboundMessage;
import io.electrica.sdk.java8.core.message.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of the Http module that utilizes {@link OkHttpClient} for connecting
 * with and reading from Electrica.io services.
 * <p>
 * Supported only one assigned {@link Electrica} instance.
 */
@Slf4j
public class SingleInstanceHttpModule implements HttpModule {

    public static final String API_URL = "https://api.electrica.io";
    public static final String INVOKE_PATH = "/v1/sdk/invoke-sync";
    public static final String CONNECTIONS_PATH = "/v1/sdk/connections";
    public static final String MESSAGE_RESULT_PATH = "/v1/webhooks/messages/result";
    public static final String EVENT_DISPATCHER_THREAD_GROUP = "electrica-sdk-event-dispatcher";

    static final String AUTHORIZATION = "Authorization";
    private static final MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");

    private final String apiUrl;

    private volatile String authorizationHeader;
    private volatile WebSocketHandler webSocketHandler;
    private volatile InstanceEventDispatcher eventDispatcher;

    private volatile Gson gson;
    private volatile OkHttpClient httpClient;
    private volatile RetryStrategy webSocketRetryStrategy;
    private volatile ExecutorService eventExecutor;
    private volatile long eventDispatcherTerminationTimeout = TimeUnit.SECONDS.toMillis(10);
    private volatile long webSocketTerminationTimeout = TimeUnit.SECONDS.toMillis(5);

    private boolean closed = false;

    public SingleInstanceHttpModule() {
        this(API_URL);
    }

    public SingleInstanceHttpModule(String apiUrl) {
        this.apiUrl = requireNonNull(apiUrl, "apiUrl");
    }

    private synchronized void checkClosed() {
        if (closed) {
            throw new IllegalStateException("SingleInstanceHttpModule has been closed");
        }
    }

    /**
     * Specify custom {@link Gson} instance, otherwise {@link #createDefaultGson()} will used.
     * Make sense only until {@link #initialize(UUID, String, String)} invoked.
     */
    public void setGson(Gson gson) {
        this.gson = gson;
    }

    /**
     * Method to create default {@link Gson} instance if nothing custom has been specified.
     *
     * @see #setGson(Gson)
     */
    protected Gson createDefaultGson() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(InboundMessage.TYPE_ADAPTER_FACTORY)
                .create();
    }

    /**
     * Specify custom {@link OkHttpClient} instance, otherwise {@link #createDefaultHttpClient()} will used.
     * Make sense only until {@link #initialize(UUID, String, String)} invoked.
     */
    public void setHttpClient(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Method to create default {@link OkHttpClient} instance if nothing custom has been specified.
     *
     * @see #setHttpClient(OkHttpClient)
     */
    protected OkHttpClient createDefaultHttpClient() {
        return new OkHttpClient.Builder()
                .pingInterval(30, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * Specify custom {@link RetryStrategy} instance, otherwise {@link #createDefaultRetryStrategy()} will used.
     * Make sense only until {@link #initialize(UUID, String, String)} invoked.
     */
    public void setWebSocketRetryStrategy(RetryStrategy webSocketRetryStrategy) {
        this.webSocketRetryStrategy = webSocketRetryStrategy;
    }

    /**
     * Method to create default {@link RetryStrategy} instance if nothing custom has been specified.
     *
     * @see #setWebSocketRetryStrategy(RetryStrategy)
     */
    protected RetryStrategy createDefaultRetryStrategy() {
        return new RetryStrategy.Linear(30, 5, TimeUnit.SECONDS);
    }

    /**
     * Specify custom {@link ExecutorService} instance for websocket events delivery, otherwise
     * {@link #createDefaultEventExecutor()} will used.
     * Make sense only until {@link #initialize(UUID, String, String)} invoked.
     */
    public void setEventExecutor(ExecutorService eventExecutor) {
        this.eventExecutor = eventExecutor;
    }

    /**
     * Method to create default {@link ExecutorService} instance if nothing custom has been specified.
     *
     * @see #setEventExecutor(ExecutorService)
     */
    protected ExecutorService createDefaultEventExecutor() {
        ThreadGroup group = new ThreadGroup(EVENT_DISPATCHER_THREAD_GROUP);
        return Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(group, runnable);
            thread.setDaemon(false);
            return thread;
        });
    }

    /**
     * Specify timeout to gracefully terminate event dispatcher tasks.
     *
     * @param eventDispatcherTerminationTimeout timeout in millis
     * @see InstanceEventDispatcher#close()
     */
    public void setEventDispatcherTerminationTimeout(long eventDispatcherTerminationTimeout) {
        this.eventDispatcherTerminationTimeout = eventDispatcherTerminationTimeout;
    }

    /**
     * Specify timeout to gracefully terminate OkHttp client tasks.
     *
     * @param webSocketTerminationTimeout timeout in millis
     * @see WebSocketHandler#close()
     */
    public void setWebSocketTerminationTimeout(long webSocketTerminationTimeout) {
        this.webSocketTerminationTimeout = webSocketTerminationTimeout;
    }

    @Override
    public List<ConnectionInfo> getConnections(UUID instanceId, @Nullable String name, String ern) throws IOException {
        checkClosed();

        if (log.isDebugEnabled()) {
            log.debug("Fetch connections for {} connector with filter by name '{}'", ern, name);
        }

        String url = apiUrl + CONNECTIONS_PATH;
        HttpUrl.Builder urlBuilder = HttpUrl.get(url).newBuilder()
                .addQueryParameter("ern", ern);
        if (name != null) {
            urlBuilder.addQueryParameter("connectionName", name);
        }

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(urlBuilder.build())
                .header(AUTHORIZATION, authorizationHeader)
                .build();

        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Invocation failed: " + response);
        }

        String json = requireNonNull(response.body(), "body").string();
        return gson.fromJson(json, new TypeToken<List<ConnectionDto>>() {
        }.getType());
    }

    @Override
    public void sendMessageResult(UUID instanceId, Message message, @Nullable String result) throws IOException {
        checkClosed();

        String url = apiUrl + MESSAGE_RESULT_PATH;
        MessageResultDto resultDto = MessageResultDto.of(instanceId, message, result);
        String jsonBody = gson.toJson(resultDto);

        if (log.isDebugEnabled()) {
            log.debug("Send result:\n{}\n for message:\n{}", jsonBody, gson.toJson(((MessageImpl) message).getDto()));
        }

        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .url(url)
                .header(AUTHORIZATION, authorizationHeader)
                .post(RequestBody.create(APPLICATION_JSON, jsonBody))
                .build();

        Response response = httpClient.newCall(httpRequest).execute();
        if (!Objects.equals(response.code(), 202)) {
            throw new IOException("Request exception: " + response);
        }
    }

    @Override
    public UUID addMessageListener(UUID instanceId, Predicate<Message> filter, Consumer<Message> listener) {
        checkClosed();

        return eventDispatcher.addMessageListener(filter, listener);
    }

    @Override
    public void removeMessageListener(UUID instanceId, UUID listenerId) {
        checkClosed();

        eventDispatcher.removeMessageListener(listenerId);
    }

    @Override
    public void initialize(UUID instanceId, String instanceName, String accessKey) {
        checkClosed();

        // Init defaults
        if (gson == null) {
            gson = createDefaultGson();
        }
        if (httpClient == null) {
            httpClient = createDefaultHttpClient();
        }
        if (webSocketRetryStrategy == null) {
            webSocketRetryStrategy = createDefaultRetryStrategy();
        }
        if (eventExecutor == null) {
            eventExecutor = createDefaultEventExecutor();
        }

        // Create services
        authorizationHeader = createAuthorizationHeader(accessKey);
        eventDispatcher = createEventDispatcher();
        webSocketHandler = createWebSocketHandler(instanceId, instanceName);
    }

    protected String createAuthorizationHeader(String accessKey) {
        return "Bearer " + accessKey;
    }

    protected InstanceEventDispatcher createEventDispatcher() {
        return new InstanceEventDispatcher(gson, eventExecutor, eventDispatcherTerminationTimeout);
    }

    protected WebSocketHandler createWebSocketHandler(UUID instanceId, String instanceName) {
        return new WebSocketHandler(
                gson,
                httpClient,
                webSocketRetryStrategy,
                eventDispatcher,
                webSocketTerminationTimeout,
                apiUrl,
                instanceId,
                instanceName,
                authorizationHeader
        );
    }

    @Override
    public <R> void submitJob(
            UUID instanceId,
            Request request,
            Class<R> resultType,
            Callback<R> callback
    ) throws IOException {
        checkClosed();

        String url = apiUrl + INVOKE_PATH;
        String jsonBody = gson.toJson(request);

        if (log.isDebugEnabled()) {
            log.debug("Submit job:\n{}", jsonBody);
        }

        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .url(url)
                .header(AUTHORIZATION, authorizationHeader)
                .post(RequestBody.create(APPLICATION_JSON, jsonBody))
                .build();

        Response response = httpClient.newCall(httpRequest).execute();
        if (!response.isSuccessful()) {
            throw new IOException("Request exception: " + response);
        }

        String resultJson = requireNonNull(response.body()).string();
        ResultMessage resultMessage = gson.fromJson(resultJson, ResultMessage.class);
        if (resultMessage.getSuccess()) {
            R toReturn = resultType.equals(Void.class) ? null : gson.fromJson(resultMessage.getResult(), resultType);
            callback.onResponse(toReturn);
        } else {
            callback.onFailure(resultMessage.getError().asException());
        }
    }

    @Override
    public void close(UUID instanceId) throws Exception {
        close(); // the same for current implementation
    }

    @Override
    public synchronized void close() throws Exception {
        if (!closed) {
            if (webSocketHandler != null) {
                webSocketHandler.close();
            }
            if (eventDispatcher != null) {
                eventDispatcher.close();
            }
            closed = true;
        }
    }
}
