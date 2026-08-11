package org.gh7035.ieumson.domain.progress.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.global.entity.BaseEntity;

import java.time.LocalDate;

@Entity
@Builder(access = AccessLevel.PRIVATE)
@Table(
        name = "daily_learning_result",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "learned_date"}) //id + 날짜 조합은 unique
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DailyLearningResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "learned_date", nullable = false)
    private LocalDate learnedDate; // 학습한 날짜

    @Column(name = "words_completed", nullable = false)
    private Integer wordsCompleted; // 이날 완료한 단어 수

    @Column(name = "sentences_completed", nullable = false)
    private Integer sentencesCompleted; // 이날 완료한 문장 수

    @Column(name = "streak_count", nullable = false)
    private Integer streakCount; // 이날 기준 연속 스트릭

    public static DailyLearningResult start(Member member, LocalDate learnedDate, int streakCount) {
        return DailyLearningResult.builder()
                .member(member)
                .learnedDate(learnedDate)
                .wordsCompleted(0)
                .sentencesCompleted(0)
                .streakCount(streakCount)
                .build();
    }

    public void addWords(int count) {
        this.wordsCompleted += requirePositive(count);
    }

    public void addSentences(int count) {
        this.sentencesCompleted += requirePositive(count);
    }

    private static int requirePositive(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("학습 완료 수는 1 이상씩만 누적할 수 있습니다: " + count);
        }
        return count;
    }
}