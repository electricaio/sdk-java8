package io.electrica.sdk.java8.hackerrank.v3.tests.v1;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.Connector;
import io.electrica.sdk.java8.api.Electrica;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.api.http.ConnectionInfo;
import io.electrica.sdk.java8.api.http.HttpModule;
import io.electrica.sdk.java8.api.http.Request;
import io.electrica.sdk.java8.hackerrank.v3.candidates.v1.model.HackerRankV3CandidatesAction;
import io.electrica.sdk.java8.hackerrank.v3.candidates.v1.model.HackerRankV3TestCandidateInvite;
import io.electrica.sdk.java8.hackerrank.v3.candidates.v1.model.HackerRankV3TestCandidatePayload;
import io.electrica.sdk.java8.hackerrank.v3.candidates.v1.model.HackerRankV3TestInvitationResponse;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentMatchers;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HackerRankV3CandidatesTest {

    public static final int TEST_ID = 123;
    private static final long TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private static Electrica electrica;

    private static HttpModule httpModule = mock(HttpModule.class);
    private static HackerRankV3Candidates candidates;

    @BeforeAll
    static void setUp() throws IOException {
        electrica = Electrica.instance(httpModule, "key");
        mockConnection();
    }


    @AfterAll
    static void tearDown() throws Exception {
        electrica.close();
    }

    private static void mockConnection() throws IOException {
        doAnswer(invocation -> {
            ConnectionInfo connectionInfo = mock(ConnectionInfo.class);
            when(connectionInfo.getId()).thenReturn(1L);
            when(connectionInfo.getName()).thenReturn(Connection.DEFAULT_NAME);
            return Collections.singletonList(connectionInfo);
        })
                .when(httpModule)
                .getConnections(
                        eq(electrica.getInstanceId()),
                        eq(Connection.DEFAULT_NAME),
                        eq(HackerRankV3Tests.ERN)
                );
    }

    @BeforeEach
    void setUpHackerRank() {
        Connector connector = electrica.connector(HackerRankV3Candidates.ERN);
        Connection connection = connector.defaultConnection();
        candidates = new HackerRankV3Candidates(connection);
    }

    @AfterEach
    void tearDownHackerRank() throws Exception {
        candidates.close();
        assertTrue(candidates.getConnection().isClosed());
    }

    private <R> void assertInvitationActionAndPayload(R response, HackerRankV3TestCandidateInvite body)
            throws IOException {
        doAnswer(invocation -> {
            Request request = invocation.getArgument(1);
            HackerRankV3TestCandidatePayload payload = (HackerRankV3TestCandidatePayload) request.getPayload();
            assertEquals(TEST_ID, payload.getTestId().intValue());
            assertEquals(body, payload.getBody());
            assertEquals(HackerRankV3CandidatesAction.INVITECANDIDATE.getValue(), request.getAction());

            Callback<R> rh = invocation.getArgument(3);
            rh.onResponse(response);
            return null;
        })
                .when(httpModule)
                .submitJob(
                        eq(electrica.getInstanceId()),
                        any(Request.class),
                        any(),
                        ArgumentMatchers.<Callback<R>>any());
    }

    private HackerRankV3TestCandidateInvite createInvitation() {
        return new HackerRankV3TestCandidateInvite().email("test@email.com");
    }

    @Test
    void inviteCandidateAsynchronously() throws Exception {
        HackerRankV3TestInvitationResponse response = new HackerRankV3TestInvitationResponse();
        String id = "hacker rank show response";
        response.setId(id);

        HackerRankV3TestCandidateInvite body = createInvitation();

        assertInvitationActionAndPayload(response, body);

        BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
        candidates.invite(TEST_ID, body, new Callback<HackerRankV3TestInvitationResponse>() {
            @Override
            public void onResponse(HackerRankV3TestInvitationResponse result) {
                queue.add(result);
            }

            @Override
            public void onFailure(IntegrationException exception) {
                queue.add(exception);
            }
        });

        HackerRankV3TestInvitationResponse result = (HackerRankV3TestInvitationResponse) queue.poll(TIMEOUT,
                TimeUnit.MILLISECONDS);
        assertEquals(id, result.getId());
    }


    @Test
    void inviteCandidateAsynchronouslyWithActionAndPayload() throws Exception {
        HackerRankV3TestCandidateInvite body = createInvitation();
        assertInvitationActionAndPayload(new HackerRankV3TestInvitationResponse(), body);
        candidates.invite(TEST_ID, body, mock(Callback.class));
    }

    @Test
    void inviteCandidateSynchronously() throws Exception {
        String id = "hacker rank show response";
        HackerRankV3TestInvitationResponse response = new HackerRankV3TestInvitationResponse();
        response.setId(id);
        HackerRankV3TestCandidateInvite body = createInvitation();

        assertInvitationActionAndPayload(response, body);
        HackerRankV3TestInvitationResponse result = candidates.invite(TEST_ID, body, TIMEOUT, TimeUnit.SECONDS);
        assertEquals(result.getId(), id);
    }

    @Test
    void inviteCandidateSynchronouslyWithActionAndPayload() throws Exception {
        HackerRankV3TestCandidateInvite body = createInvitation();
        assertInvitationActionAndPayload(new HackerRankV3TestInvitationResponse(), body);
        candidates.invite(TEST_ID, body, TIMEOUT, TimeUnit.MILLISECONDS);
    }

}
