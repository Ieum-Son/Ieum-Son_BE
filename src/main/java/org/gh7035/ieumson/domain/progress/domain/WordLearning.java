package org.gh7035.ieumson.domain.progress.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.global.entity.BaseEntity;

import java.time.LocalDateTime;


@Entity
@Builder
@Table(
        name = "word_learning",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "word_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WordLearning extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;


    @Column(name = "best_accuracy", nullable = false)
    private Float bestAccuracy; // 최고 정확도 (0~100)

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount; // 시도 횟수

    @Column(name = "is_mastered", nullable = false)
    private Boolean isMastered; // 90% 넘겨서 완료했는가

    @Column(name = "last_practiced_at", nullable = false)
    private LocalDateTime lastPracticedAt; // 마지막 학습/복습 시각

    @Column(name = "mastered_at")
    private LocalDateTime masteredAt; // 마스터한 시각 (복습 간격 계산용)

    public static WordLearning start(Member member, Word word) {
        return WordLearning.builder()
                .member(member)
                .word(word)
                .bestAccuracy(0f)
                .attemptCount(0)
                .isMastered(false)
                .lastPracticedAt(LocalDateTime.now())
                .build();
    }

    /**
     * @return 이번 시도로 새로 마스터했으면 true
     */
    public boolean recordPractice(float accuracy) {
        boolean alreadyMastered = Boolean.TRUE.equals(this.isMastered);
        this.attemptCount += 1;
        this.lastPracticedAt = LocalDateTime.now();
        if (this.bestAccuracy == null || accuracy > this.bestAccuracy) {
            this.bestAccuracy = accuracy;
        }
        if (!alreadyMastered && accuracy >= 90f) {
            this.isMastered = true;
            this.masteredAt = this.lastPracticedAt;
            return true;
        }
        return false;
    }
}
