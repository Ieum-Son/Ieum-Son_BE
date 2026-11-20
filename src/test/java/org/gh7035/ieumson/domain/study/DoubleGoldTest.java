package org.gh7035.ieumson.domain.study;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.enums.Role;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.gh7035.ieumson.domain.progress.domain.GoldTransaction;
import org.gh7035.ieumson.domain.progress.domain.LearningPolicy;
import org.gh7035.ieumson.domain.progress.domain.repository.DailyLearningResultRepository;
import org.gh7035.ieumson.domain.progress.domain.repository.GoldTransactionRepository;
import org.gh7035.ieumson.domain.study.service.CompleteLearningService;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(LearningSessionAttemptFlowTest.TestInfraConfig.class)
class DoubleGoldTest {

    @Autowired
    private CompleteLearningService completeLearningService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private DailyLearningResultRepository dailyLearningResultRepository;
    @Autowired
    private GoldTransactionRepository goldTransactionRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private S3Client s3Client;

    @DynamicPropertySource
    static void isolateDatabase(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:double-gold;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        );
    }

    @Test
    void concurrentCompleteAwardsGoldOnce() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Long memberId = tx.execute(status -> {
            Member member = memberRepository.save(Member.builder()
                    .email("gold-" + suffix + "@gmail.com")
                    .loginId("gold-" + suffix)
                    .name("홍길동")
                    .password("encoded")
                    .emailVerified(true)
                    .role(Role.USER)
                    .build());
            DailyLearningResult today = DailyLearningResult.start(
                    member,
                    LocalDate.now(LearningPolicy.ZONE),
                    1,
                    List.of(),
                    List.of(),
                    List.of()
            );
            today.addWords(5);
            dailyLearningResultRepository.save(today);
            return member.getId();
        });

        Member member = memberRepository.findById(memberId).orElseThrow();
        CustomUserDetails userDetails = new CustomUserDetails(member);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("동시 시작 대기 시간이 지났습니다.");
                    }
                    completeLearningService.execute(userDetails);
                    return null;
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        tx.executeWithoutResult(status -> {
            Member saved = memberRepository.findById(memberId).orElseThrow();
            List<GoldTransaction> transactions = goldTransactionRepository
                    .findByMemberOrderByCreatedAtDesc(saved, PageRequest.of(0, 10))
                    .getContent();
            assertThat(saved.getNuggetBalance()).isEqualTo(5);
            assertThat(transactions).hasSize(1);
            assertThat(transactions.getFirst().getAmount()).isEqualTo(5);
        });
    }
}
