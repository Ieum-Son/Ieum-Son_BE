package org.gh7035.ieumson.infrastructure.ticket;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DetectTicketIssuer {

    private final DetectTicketProperties properties;

    public IssuedTicket issue(DailyLearningResult session, List<Word> words, List<Word> reviews) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = resolveExpiry(now, session.getLearnedDate()).truncatedTo(ChronoUnit.SECONDS);

        String token = Jwts.builder()
                .subject(String.valueOf(session.getMember().getId()))
                .claim("sid", session.getId())
                .claim("date", session.getLearnedDate().toString())
                .claim("words", toClaims(words))
                .claim("reviews", toClaims(reviews))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();

        return new IssuedTicket(
                token,
                LocalDateTime.ofInstant(expiresAt, LearningPolicy.ZONE)
        );
    }

    private Instant resolveExpiry(Instant now, LocalDate learnedDate) {
        Instant configured = now.plusMillis(properties.ticketExpiry());
        Instant endOfDay = learnedDate.atTime(LocalTime.MAX).atZone(LearningPolicy.ZONE).toInstant();
        return configured.isBefore(endOfDay) ? configured : endOfDay;
    }

    private SecretKey signingKey() {
        return new SecretKeySpec(properties.ticketSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static List<Map<String, Object>> toClaims(List<Word> words) {
        return words.stream()
                .map(word -> Map.<String, Object>of(
                        "wordId", word.getId(),
                        "word", word.getWord()
                ))
                .toList();
    }

    public record IssuedTicket(String token, LocalDateTime expiresAt) {
    }
}
