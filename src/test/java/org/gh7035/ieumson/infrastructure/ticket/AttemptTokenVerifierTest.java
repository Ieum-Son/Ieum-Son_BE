package org.gh7035.ieumson.infrastructure.ticket;

import io.jsonwebtoken.Jwts;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AttemptTokenVerifierTest {

    private static final String SECRET = "test-ticket-secret-that-is-at-least-32-bytes-long";

    private final AttemptTokenVerifier verifier = new AttemptTokenVerifier(new DetectTicketProperties(SECRET, 60_000L));

    @Test
    void issueAndVerifyRoundTrip() {
        String token = verifier.issue(10L, 99L, 7L, true);

        AttemptClaims claims = verifier.verify(token);

        assertThat(claims.memberId()).isEqualTo(10L);
        assertThat(claims.sessionId()).isEqualTo(99L);
        assertThat(claims.wordId()).isEqualTo(7L);
        assertThat(claims.correct()).isTrue();
    }

    @Test
    void sessionTicketWithoutTypIsRejected() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        String sessionTicket = Jwts.builder()
                .subject("10")
                .claim("sid", 99L)
                .claim("wordId", 7L)
                .claim("correct", true)
                .claim("words", java.util.List.of())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> verifier.verify(sessionTicket))
                .isInstanceOf(IeumException.class)
                .extracting(error -> ((IeumException) error).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_TOKEN);
    }

    @Test
    void longSecretStillSignsHs256() {
        String secret = "x".repeat(64);
        AttemptTokenVerifier longVerifier = new AttemptTokenVerifier(new DetectTicketProperties(secret, 60_000L));

        String token = longVerifier.issue(1L, 2L, 3L, true);

        assertThat(header(token)).contains("\"alg\":\"HS256\"");
        assertThat(longVerifier.verify(token).wordId()).isEqualTo(3L);
    }

    @Test
    void tokenIssuedWithinClockSkewStillVerifies() {
        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Instant issuedAt = Instant.now().plusSeconds(8);
        String token = Jwts.builder()
                .subject("10")
                .claim("typ", AttemptTokenVerifier.TYPE)
                .claim("sid", 99L)
                .claim("wordId", 7L)
                .claim("correct", true)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThat(verifier.verify(token).wordId()).isEqualTo(7L);
    }

    @Test
    void pyJwtAttemptTokenVerifies() {
        AttemptClaims claims = verifier.verify(PYJWT_ATTEMPT_TOKEN);

        assertThat(claims.memberId()).isEqualTo(10L);
        assertThat(claims.sessionId()).isEqualTo(99L);
        assertThat(claims.wordId()).isEqualTo(7L);
        assertThat(claims.correct()).isTrue();
    }

    private static String header(String token) {
        String segment = token.split("\\.")[0];
        int padding = (4 - segment.length() % 4) % 4;
        byte[] decoded = Base64.getUrlDecoder().decode(segment + "=".repeat(padding));
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private static final String PYJWT_ATTEMPT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0eXAiOiJhdHRlbXB0Iiwic3ViIjoiMTAiLCJzaWQiOjk5LCJ3b3JkSWQiOjcsImNvcnJlY3QiOnRydWUsImlhdCI6MTc2MTIwMDAwMCwiZXhwIjo0MTAyNDQ0ODAwfQ._ECCujNsA0d-fPRfkisXHpqPlNttA_vK_-wfWFbhU_M";
}