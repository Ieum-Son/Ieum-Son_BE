package org.gh7035.ieumson.infrastructure.ticket;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.enums.Role;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetectTicketIssuerTest {

    private static final String SECRET = "test-ticket-secret-that-is-at-least-32-bytes-long";

    @Test
    void issueSignsExpectedClaimsWithoutTyp() {
        DetectTicketIssuer issuer = new DetectTicketIssuer(new DetectTicketProperties(SECRET, 60_000L));
        Member member = member(10L);
        DailyLearningResult session = session(member, 99L, LocalDate.now(LearningPolicy.ZONE).plusDays(1), List.of(1L, 2L), List.of(3L));
        Word hello = word(1L, "안녕");
        Word thanks = word(2L, "고마워");
        Word review = word(3L, "사랑");

        DetectTicketIssuer.IssuedTicket issued = issuer.issue(session, List.of(hello, thanks), List.of(review));

        Claims claims = parse(issued.token());
        assertThat(claims.getSubject()).isEqualTo("10");
        assertThat(asLong(claims.get("sid"))).isEqualTo(99L);
        assertThat(claims.get("date")).isEqualTo(session.getLearnedDate().toString());
        assertThat(claims.get("typ")).isNull();
        assertWordClaims(claims.get("words", List.class), 1L, "안녕", 2L, "고마워");
        assertWordClaims(claims.get("reviews", List.class), 3L, "사랑");
        assertThat(issued.expiresAt()).isEqualTo(
                LocalDateTime.ofInstant(claims.getExpiration().toInstant(), LearningPolicy.ZONE)
        );
    }

    @Test
    void issueUsesConfiguredExpiryWhenItIsBeforeEndOfLearnedDate() {
        DetectTicketIssuer issuer = new DetectTicketIssuer(new DetectTicketProperties(SECRET, 1_000L));
        LocalDate learnedDate = LocalDate.now(LearningPolicy.ZONE).plusDays(1);
        DailyLearningResult session = session(member(1L), 1L, learnedDate, List.of(), List.of());

        Instant before = Instant.now();
        DetectTicketIssuer.IssuedTicket issued = issuer.issue(session, List.of(), List.of());
        Instant after = Instant.now();

        Instant expiresAt = issued.expiresAt().atZone(LearningPolicy.ZONE).toInstant();
        assertThat(expiresAt).isAfterOrEqualTo(before.plusMillis(1_000).truncatedTo(ChronoUnit.SECONDS));
        assertThat(expiresAt).isBeforeOrEqualTo(after.plusSeconds(2));
    }

    @Test
    void issueCapsExpiryAtEndOfLearnedDate() {
        DetectTicketIssuer issuer = new DetectTicketIssuer(new DetectTicketProperties(SECRET, 48 * 60 * 60 * 1000L));
        LocalDate yesterday = LocalDate.now(LearningPolicy.ZONE).minusDays(1);
        DailyLearningResult session = session(member(1L), 1L, yesterday, List.of(), List.of());

        DetectTicketIssuer.IssuedTicket issued = issuer.issue(session, List.of(), List.of());

        Instant endOfDay = yesterday.atTime(LocalTime.MAX)
                .atZone(LearningPolicy.ZONE)
                .toInstant()
                .truncatedTo(ChronoUnit.SECONDS);
        assertThat(issued.expiresAt()).isEqualTo(LocalDateTime.ofInstant(endOfDay, LearningPolicy.ZONE));
    }

    @Test
    void propertiesDefaultExpiryWhenMissingOrInvalid() {
        assertThat(new DetectTicketProperties(SECRET, null).ticketExpiry()).isEqualTo(14_400_000L);
        assertThat(new DetectTicketProperties(SECRET, 0L).ticketExpiry()).isEqualTo(14_400_000L);
        assertThat(new DetectTicketProperties(SECRET, -1L).ticketExpiry()).isEqualTo(14_400_000L);
        assertThat(new DetectTicketProperties(SECRET, 3_000L).ticketExpiry()).isEqualTo(3_000L);
        assertThatThrownBy(() -> new DetectTicketProperties("too-short", 1_000L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Claims parse(String token) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static Member member(Long id) {
        Member member = Member.builder()
                .email("dino@gmail.com")
                .loginId("dino")
                .name("홍길동")
                .password("encoded")
                .emailVerified(true)
                .role(Role.USER)
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private static DailyLearningResult session(
            Member member,
            Long id,
            LocalDate learnedDate,
            List<Long> wordIds,
            List<Long> reviewIds
    ) {
        DailyLearningResult session = DailyLearningResult.start(
                member, learnedDate, 1, wordIds, List.of(), reviewIds);
        ReflectionTestUtils.setField(session, "id", id);
        return session;
    }

    private static Word word(Long id, String label) {
        Word word = Word.builder()
                .word(label)
                .definition(label)
                .videoLink("https://example.com/" + id)
                .category("daily")
                .difficulty(1)
                .build();
        ReflectionTestUtils.setField(word, "id", id);
        return word;
    }

    private static long asLong(Object value) {
        return ((Number) value).longValue();
    }

    private static void assertWordClaims(List<?> claims, Object... expected) {
        assertThat(claims).hasSize(expected.length / 2);
        for (int i = 0; i < claims.size(); i++) {
            Map<?, ?> claim = (Map<?, ?>) claims.get(i);
            assertThat(asLong(claim.get("wordId"))).isEqualTo(((Number) expected[i * 2]).longValue());
            assertThat(claim.get("word")).isEqualTo(expected[i * 2 + 1]);
        }
    }
}
