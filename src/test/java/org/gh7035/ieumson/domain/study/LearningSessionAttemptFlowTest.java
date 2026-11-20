package org.gh7035.ieumson.domain.study;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.enums.Role;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
import org.gh7035.ieumson.domain.study.domain.Chapter;
import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.domain.study.domain.repository.ChapterRepository;
import org.gh7035.ieumson.domain.study.domain.repository.LessonRepository;
import org.gh7035.ieumson.domain.study.domain.repository.WordRepository;
import org.gh7035.ieumson.global.security.jwt.JwtTokenProvider;
import org.gh7035.ieumson.infrastructure.ticket.AttemptTokenVerifier;
import org.gh7035.ieumson.infrastructure.ticket.DetectTicketProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(LearningSessionAttemptFlowTest.TestInfraConfig.class)
class LearningSessionAttemptFlowTest {

    private static final String LOGIN_ID = "dino";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private DetectTicketProperties ticketProperties;
    @Autowired
    private AttemptTokenVerifier attemptTokenVerifier;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private WordRepository wordRepository;
    @Autowired
    private WordLearningRepository wordLearningRepository;

    @MockitoBean
    private S3Client s3Client;

    private Member member;
    private List<Word> words;
    private long currentSessionId;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.builder()
                .email("dino@gmail.com")
                .loginId(LOGIN_ID)
                .name("홍길동")
                .password(passwordEncoder.encode("password12!"))
                .emailVerified(true)
                .role(Role.USER)
                .build());

        Chapter chapter = chapterRepository.save(Chapter.builder()
                .title("기초")
                .description("기초 수화")
                .orderNumber(1)
                .build());
        Lesson lesson = lessonRepository.save(Lesson.builder()
                .name("인사")
                .chapter(chapter)
                .orderNumber(1)
                .description("인사 표현")
                .build());

        words = new java.util.ArrayList<>();
        List<String> labels = List.of("가능", "곳", "가입", "가정", "가지다", "각각", "공부", "같다", "개인");
        for (int i = 1; i <= labels.size(); i++) {
            words.add(wordRepository.save(Word.builder()
                    .word(labels.get(i - 1))
                    .definition("뜻" + i)
                    .videoLink("https://example.com/" + i)
                    .category("daily")
                    .difficulty(1)
                    .lesson(lesson)
                    .build()));
        }

        for (int i = 0; i < 3; i++) {
            WordLearning learning = WordLearning.start(member, words.get(i));
            learning.recordPractice(95f);
            wordLearningRepository.save(learning);
        }
        wordLearningRepository.flush();
        wordRepository.flush();
    }

    @Test
    void startSessionThenSubmitLearnAndReviewAttempts() throws Exception {
        String accessToken = jwtTokenProvider.generateAccessToken(LOGIN_ID);

        MvcResult sessionResult = mockMvc.perform(get("/api/progress/daily")
                        .header("Authorization", "Bearer " + accessToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").isNumber())
                .andExpect(jsonPath("$.sessionTicket").isString())
                .andExpect(jsonPath("$.expiresAt").isString())
                .andExpect(jsonPath("$.words.length()").value(5))
                .andExpect(jsonPath("$.words[0].wordId").isNumber())
                .andExpect(jsonPath("$.words[0].word").value("가정"))
                .andExpect(jsonPath("$.words[0].definition").value("뜻4"))
                .andExpect(jsonPath("$.words[0].videoUrl").value("https://example.com/4"))
                .andExpect(jsonPath("$.words[4].word").value("같다"))
                .andExpect(jsonPath("$.reviews.length()").value(3))
                .andReturn();

        String body = sessionResult.getResponse().getContentAsString();
        long sessionId = ((Number) json(body, "$.sessionId")).longValue();
        currentSessionId = sessionId;
        String ticket = json(body, "$.sessionTicket");
        assertThat((List<String>) json(body, "$.words[*].word")).containsExactly(
                "가정", "가지다", "각각", "공부", "같다"
        );
        assertThat((List<String>) json(body, "$.reviews[*].word")).containsExactlyInAnyOrder(
                "가능", "곳", "가입"
        );
        assertThat(((Number) json(body, "$.words[0].wordId")).longValue()).isEqualTo(words.get(3).getId());
        Claims claims = parseTicket(ticket);

        assertThat(claims.getSubject()).isEqualTo(String.valueOf(member.getId()));
        assertThat(((Number) claims.get("sid")).longValue()).isEqualTo(sessionId);
        assertTicketWords(claims.get("words", List.class), words.subList(3, 8));
        assertTicketWords(claims.get("reviews", List.class), words.subList(0, 3));

        Long learnWordId = words.get(3).getId();
        mockMvc.perform(post("/internal/words/{wordId}/attempts", learnWordId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/internal/words/{wordId}/attempts", learnWordId)
                        .header("Authorization", "Bearer " + ticket))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/internal/words/{wordId}/attempts", learnWordId)
                        .header("Authorization", "Bearer " + attemptToken(learnWordId, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wordId").value(learnWordId.intValue()))
                .andExpect(jsonPath("$.passed").value(false));

        mockMvc.perform(post("/internal/words/{wordId}/attempts", learnWordId)
                        .header("Authorization", "Bearer " + attemptToken(learnWordId, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wordId").value(learnWordId.intValue()))
                .andExpect(jsonPath("$.passed").value(true));

        Long reviewWordId = words.get(0).getId();
        mockMvc.perform(post("/internal/words/{wordId}/attempts", reviewWordId)
                        .header("Authorization", "Bearer " + attemptToken(reviewWordId, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wordId").value(reviewWordId.intValue()))
                .andExpect(jsonPath("$.passed").value(true));

        Long outsideWordId = words.get(8).getId();
        mockMvc.perform(post("/internal/words/{wordId}/attempts", outsideWordId)
                        .header("Authorization", "Bearer " + attemptToken(outsideWordId, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("오늘 학습에 배정되지 않은 단어입니다."));
    }

    @Test
    void getDailyRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/progress/daily").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
    }

    private String attemptToken(long wordId, boolean correct) {
        return attemptTokenVerifier.issue(member.getId(), currentSessionId, wordId, correct);
    }

    private static void assertTicketWords(List<?> claims, List<Word> expected) {
        List<String> actual = claims.stream()
                .map(item -> {
                    Map<?, ?> claim = (Map<?, ?>) item;
                    return ((Number) claim.get("wordId")).longValue() + ":" + claim.get("word");
                })
                .toList();
        List<String> wanted = expected.stream()
                .map(word -> word.getId() + ":" + word.getWord())
                .toList();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(wanted);
    }

    private Claims parseTicket(String ticket) {
        SecretKey key = Keys.hmacShaKeyFor(ticketProperties.ticketSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(ticket)
                .getPayload();
    }

    private static <T> T json(String body, String path) {
        return com.jayway.jsonpath.JsonPath.read(body, path);
    }

    @TestConfiguration
    static class TestInfraConfig {

        @Bean
        @Primary
        RedisConnectionFactory redisConnectionFactory() {
            return mock(RedisConnectionFactory.class);
        }

        @Bean
        @Primary
        StringRedisTemplate stringRedisTemplate() {
            Map<String, String> store = new ConcurrentHashMap<>();
            ValueOperations<String, String> values = mock(ValueOperations.class);
            when(values.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                    .thenAnswer(invocation -> store.putIfAbsent(invocation.getArgument(0), invocation.getArgument(1)) == null);
            StringRedisTemplate template = mock(StringRedisTemplate.class);
            when(template.opsForValue()).thenReturn(values);
            return template;
        }
    }
}
