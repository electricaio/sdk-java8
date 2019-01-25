package io.electrica.sdk.java8.hackerrank.v3.tests.v1;

import io.electrica.sdk.java8.api.Callback;
import io.electrica.sdk.java8.api.Connection;
import io.electrica.sdk.java8.api.exception.IntegrationException;
import io.electrica.sdk.java8.hackerrank.v3.candidates.v1.model.HackerRankV3CandidatesAction;
import io.electrica.sdk.java8.hackerrank.v3.candidates.v1.model.HackerRankV3TestCandidateInvite;
import io.electrica.sdk.java8.hackerrank.v3.candidates.v1.model.HackerRankV3TestCandidatePayload;
import io.electrica.sdk.java8.hackerrank.v3.candidates.v1.model.HackerRankV3TestInvitationResponse;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@ThreadSafe
public class HackerRankV3Candidates implements AutoCloseable {

    public static final String ERN = "ern://hackerrank-v3:tests:1";

    private final Connection connection;

    public HackerRankV3Candidates(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Asynchronously invite a candidate to test.
     * <p>
     *
     * @param callback Handling async call and on success <code>HackerRankV3TestsIndexResponse</code> is returned
     * @throws IOException
     */
    public void invite(int testID, HackerRankV3TestCandidateInvite invitation, Callback<HackerRankV3TestInvitationResponse> callback) throws IOException {
        connection.submit
                (
                        HackerRankV3TestInvitationResponse.class,
                        HackerRankV3CandidatesAction.INVITECANDIDATE,
                        null,
                        new HackerRankV3TestCandidatePayload().testId(testID).body(invitation),
                        callback
                );
    }

    /**
     * Synchronously invite a candidate to test.
     *
     * @param timeout The amount of time to wait on HackerRank to return the result
     * @param unit    The unit of time to wait on HackerRank to return the result
     * @return HackerRankV3TestInvitationResponse
     * @throws IOException
     * @throws TimeoutException
     * @throws IntegrationException
     */
    public HackerRankV3TestInvitationResponse invite(int testID, HackerRankV3TestCandidateInvite invitation, long timeout, TimeUnit unit) throws IOException, TimeoutException,
            IntegrationException {
        return connection.invoke(
                HackerRankV3TestInvitationResponse.class,
                HackerRankV3CandidatesAction.INVITECANDIDATE,
                null,
                new HackerRankV3TestCandidatePayload().testId(testID).body(invitation),
                timeout,
                unit
        );
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }
}
