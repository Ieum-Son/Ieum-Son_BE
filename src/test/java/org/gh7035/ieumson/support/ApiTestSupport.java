package org.gh7035.ieumson.support;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.gh7035.ieumson.domain.auth.domain.repository.RefreshTokenRepository;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.enums.Role;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.repository.DailyLearningResultRepository;
import org.gh7035.ieumson.domain.study.domain.Chapter;
import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.domain.study.domain.repository.ChapterRepository;
import org.gh7035.ieumson.domain.study.domain.repository.LessonRepository;
import org.gh7035.ieumson.domain.study.domain.repository.WordRepository;
import org.gh7035.ieumson.global.security.jwt.JwtTokenProvider;
import org.gh7035.ieumson.infrastructure.ticket.AttemptTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.mockito.Mockito.lenient;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "api-test"})
@Transactional
@Import(ApiTestInfra.class)
public abstract class ApiTestSupport {

    protected static final String RAW_PASSWORD = "password12!";
    protected static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected JwtTokenProvider jwtTokenProvider;
    @Autowired
    protected AttemptTokenVerifier attemptTokenVerifier;
    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected MemberRepository memberRepository;
    @Autowired
    protected ChapterRepository chapterRepository;
    @Autowired
    protected LessonRepository lessonRepository;
    @Autowired
    protected WordRepository wordRepository;
    @Autowired
    protected DailyLearningResultRepository dailyLearningResultRepository;
    @Autowired
    protected InMemoryRedisStore redisStore;
    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    protected S3Client s3Client;
    @MockitoBean
    protected JavaMailSender javaMailSender;

    @DynamicPropertySource
    static void isolateDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:api-tests;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        );
    }

    @BeforeEach
    void resetExternalState() {
        redisStore.clear();
        if (refreshTokenRepository instanceof InMemoryRefreshTokenRepository inMemory) {
            inMemory.clear();
        }
        lenient().when(javaMailSender.createMimeMessage()).thenAnswer(invocation ->
                new MimeMessage(Session.getInstance(new Properties())));
    }

    protected Member saveMember(String loginId) {
        return saveMember(loginId, loginId + "@gmail.com", RAW_PASSWORD, true);
    }

    protected Member saveMember(String loginId, String email, String rawPassword, boolean emailVerified) {
        return memberRepository.save(Member.builder()
                .email(email)
                .loginId(loginId)
                .name("홍길동")
                .password(passwordEncoder.encode(rawPassword))
                .emailVerified(emailVerified)
                .role(Role.USER)
                .build());
    }

    protected String bearer(String loginId) {
        return "Bearer " + jwtTokenProvider.generateAccessToken(loginId);
    }

    protected String attemptToken(long memberId, long sessionId, long wordId, boolean correct) {
        return "Bearer " + attemptTokenVerifier.issue(memberId, sessionId, wordId, correct);
    }

    protected String uid(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    protected Lesson seedLesson(String chapterTitle, int chapterOrder, String lessonName) {
        Chapter chapter = chapterRepository.save(Chapter.builder()
                .title(chapterTitle)
                .description(chapterTitle + " 설명")
                .orderNumber(chapterOrder)
                .build());
        return lessonRepository.save(Lesson.builder()
                .name(lessonName)
                .chapter(chapter)
                .orderNumber(1)
                .description(lessonName + " 설명")
                .build());
    }

    protected Word seedWord(Lesson lesson, String text) {
        return wordRepository.save(Word.builder()
                .word(text)
                .definition(text + " 뜻")
                .videoLink("https://example.com/" + text)
                .category("daily")
                .difficulty(1)
                .lesson(lesson)
                .build());
    }

    protected DailyLearningResult savePastLearning(Member member, LocalDate date, int streakCount, boolean completed) {
        DailyLearningResult result = DailyLearningResult.start(
                member,
                date,
                streakCount,
                List.of(),
                List.of(),
                List.of()
        );
        result.addWords(1);
        if (completed) {
            result.markCompleted();
        }
        return dailyLearningResultRepository.save(result);
    }
}
