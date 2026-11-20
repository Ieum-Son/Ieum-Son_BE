package org.gh7035.ieumson.infrastructure.ticket;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class AttemptTokenVerifier {

    public static final String TYPE = "attempt";
    public static final long TTL_SECONDS = 60;
    public static final long CLOCK_SKEW_SECONDS = 10;

    private final DetectTicketProperties properties;

    public String issue(long memberId, long sessionId, long wordId, boolean correct) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return Jwts.builder()
                .subject(Long.toString(memberId))
                .claim("typ", TYPE)
                .claim("sid", sessionId)
                .claim("wordId", wordId)
                .claim("correct", correct)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(TTL_SECONDS)))
                .signWith(key(), Jwts.SIG.HS256)
                .compact();
    }

    public AttemptClaims verify(String token) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(key())
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IeumException(ErrorCode.INVALID_TOKEN);
        }
        if (!TYPE.equals(claims.get("typ", String.class)) || !(claims.get("correct") instanceof Boolean correct)) {
            throw new IeumException(ErrorCode.INVALID_TOKEN);
        }
        return new AttemptClaims(
                parseMemberId(claims.getSubject()),
                asLong(claims.get("sid")),
                asLong(claims.get("wordId")),
                correct
        );
    }

    private SecretKey key() {
        return new SecretKeySpec(properties.ticketSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static long parseMemberId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IeumException(ErrorCode.INVALID_TOKEN);
        }
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new IeumException(ErrorCode.INVALID_TOKEN);
        }
    }

    private static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IeumException(ErrorCode.INVALID_TOKEN);
    }
}
