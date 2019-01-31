package io.electrica.sdk.java8.core;

import com.google.gson.Gson;
import io.electrica.sdk.java8.core.message.AckOutboundMessage;
import io.electrica.sdk.java8.core.message.InboundMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class WebSocketHandler extends WebSocketListener implements AutoCloseable {

    public static final String WEBSOCKETS_PATH = "/v1/websockets";
    public static final String INSTANCE_ID_HEADER = "x-electrica-sdk-instance-id";
    public static final String INSTANCE_NAME_HEADER = "x-electrica-sdk-instance-name";
    public static final String INSTANCE_START_CLIENT_TIME_HEADER = "x-electrica-sdk-instance-ws-session-start-time";
    public static final String RECONNECT_THREAD_NAME = "electrica-sdk-ws-reconnect";

    protected static final int INSTANCE_CLOSE_CODE = 1000;
    protected static final String INSTANCE_CLOSE_REASON = "SDK Instance close";
    protected static final String LOG_PREFIX = "Electrica SDK WebSocket";

    protected final Gson gson;
    protected final OkHttpClient httpClient;
    protected final RetryStrategy retryStrategy;
    protected final InstanceEventDispatcher eventDispatcher;
    protected final long terminationTimeoutMillis;
    protected final Request request;
    protected final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, RECONNECT_THREAD_NAME);
                thread.setDaemon(false);
                return thread;
            }
    );

    protected final AtomicReference<WebSocket> webSocket = new AtomicReference<>();
    protected final AtomicInteger reconnectCount = new AtomicInteger();
    protected final CountDownLatch closedLatch = new CountDownLatch(1);

    protected WebSocketHandler(
            Gson gson,
            OkHttpClient httpClient,
            RetryStrategy retryStrategy,
            InstanceEventDispatcher eventDispatcher,
            long terminationTimeoutMillis,
            String apiUrl,
            UUID instanceId,
            String instanceName,
            String authorizationHeader
    ) {
        this.gson = gson;
        this.httpClient = httpClient;
        this.retryStrategy = retryStrategy;
        this.eventDispatcher = eventDispatcher;
        this.terminationTimeoutMillis = terminationTimeoutMillis;
        request = new Request.Builder()
                .url(buildEndpointUrl(apiUrl))
                .header(SingleInstanceHttpModule.AUTHORIZATION, authorizationHeader)
                .header(INSTANCE_ID_HEADER, instanceId.toString())
                .header(INSTANCE_NAME_HEADER, instanceName)
                .build();
        tryReconnect();
    }

    protected static String buildEndpointUrl(String apiUrl) {
        String url = apiUrl.contains("https") ?
                apiUrl.replaceFirst("https", "wss") :
                apiUrl.replaceFirst("http", "ws");
        return url + WEBSOCKETS_PATH;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        log.info(LOG_PREFIX + " connection established");
        if (log.isDebugEnabled()) {
            log.debug("Connection response: {}", response);
        }
        reconnectCount.set(0);
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        if (log.isDebugEnabled()) {
            log.debug(LOG_PREFIX + " got message: {}", text);
        }

        InboundMessage message = gson.fromJson(text, InboundMessage.class);
        eventDispatcher.submit(message, accepted -> {
            // TODO always send accepted ACK to avoid 'poisonous message'
            // TODO that mean we guarantee delivery to at least one instance for now
            if (!accepted) {
                accepted = true;
                log.warn("Unhandled message: " + text);
            }

            AckOutboundMessage ack = new AckOutboundMessage(message.getId(), accepted);
            String textAck = gson.toJson(ack);
            if (log.isDebugEnabled()) {
                log.debug(LOG_PREFIX + " sending ack message: {}", textAck);
            }
            webSocket.send(textAck);
        });
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        if (log.isDebugEnabled()) {
            log.debug(LOG_PREFIX + " connection closing..");
        }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        if (code == INSTANCE_CLOSE_CODE && INSTANCE_CLOSE_REASON.equalsIgnoreCase(reason)) {
            log.info(LOG_PREFIX + " connection closed");
            closedLatch.countDown();
        } else {
            log.warn(LOG_PREFIX + " connection closed: {} {}. Trying reconnect..", code, reason);
            tryReconnect();
        }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
        log.warn(LOG_PREFIX + " connection failure. Trying reconnect..", t);
        tryReconnect();
    }

    @SneakyThrows
    protected void tryReconnect() {
        long delay = 0;
        int count = reconnectCount.get();
        if (count > 0) {
            Optional<Long> delayOp = retryStrategy.getDelay(count);
            if (delayOp.isPresent()) {
                delay = delayOp.get();
            } else {
                log.error(LOG_PREFIX + " can't re-establish connection and won't get any messages or integration " +
                        "job results. Please check network or consider using correct RetryStrategy");
                close();
                return;
            }
        }

        reconnectExecutor.schedule(() -> {
            int c = reconnectCount.getAndIncrement();
            if (c > 0) {
                log.warn(LOG_PREFIX + " connect try #{}..", c + 1);
            } else {
                log.info(LOG_PREFIX + " connecting..");
            }
            Request requestWithDate = request.newBuilder()
                    .header(INSTANCE_START_CLIENT_TIME_HEADER, ZonedDateTime.now().toString())
                    .build();
            webSocket.set(httpClient.newWebSocket(requestWithDate, WebSocketHandler.this));
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() throws Exception {
        if (log.isDebugEnabled()) {
            log.debug(LOG_PREFIX + " WebSocketHandler closing..");
        }

        WebSocket ws = webSocket.get();
        if (ws != null) {
            ws.close(INSTANCE_CLOSE_CODE, INSTANCE_CLOSE_REASON);
            boolean closed = closedLatch.await(terminationTimeoutMillis, TimeUnit.MILLISECONDS);
            if (!closed) {
                log.warn("Can't await OkHttp WebSocket termination. " +
                        "Please consider increase termination timeout parameter");
            }
            ws.cancel();
        }

        httpClient.dispatcher().cancelAll();

        if (log.isDebugEnabled()) {
            log.debug(LOG_PREFIX + " WebSocketHandler closed");
        }
    }

    interface AckSender {
        void send(boolean accepted);
    }

}
