package org.gh7035.ieumson.domain.progress.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.global.entity.BaseEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assigned_word_ids", columnDefinition = "json", nullable = false)
    private List<Long> assignedWordIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assigned_sentence_ids", columnDefinition = "json", nullable = false)
    private List<Long> assignedSentenceIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assigned_review_word_ids", columnDefinition = "json", nullable = false)
    private List<Long> assignedReviewWordIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "completed_sentence_ids", columnDefinition = "json", nullable = false)
    private List<Long> completedSentenceIds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "completed_review_word_ids", columnDefinition = "json", nullable = false)
    private List<Long> completedReviewWordIds;

    @Builder.Default
    @Column(name = "completed", nullable = false)
    private Boolean completed = false;

    @Builder.Default
    @Column(name = "gold_awarded", nullable = false)
    private Boolean goldAwarded = false;

    public static DailyLearningResult start(
            Member member,
            LocalDate learnedDate,
            int streakCount,
            List<Long> assignedWordIds,
            List<Long> assignedSentenceIds,
            List<Long> assignedReviewWordIds
    ) {
        return DailyLearningResult.builder()
                .member(member)
                .learnedDate(learnedDate)
                .wordsCompleted(0)
                .sentencesCompleted(0)
                .streakCount(streakCount)
                .assignedWordIds(copyIds(assignedWordIds))
                .assignedSentenceIds(copyIds(assignedSentenceIds))
                .assignedReviewWordIds(copyIds(assignedReviewWordIds))
                .completedSentenceIds(List.of())
                .completedReviewWordIds(List.of())
                .completed(false)
                .goldAwarded(false)
                .build();
    }

    public List<Long> assignedWordIds() {
        return copyIds(assignedWordIds);
    }

    public List<Long> assignedSentenceIds() {
        return copyIds(assignedSentenceIds);
    }

    public List<Long> assignedReviewWordIds() {
        return copyIds(assignedReviewWordIds);
    }

    public List<Long> completedSentenceIds() {
        return copyIds(completedSentenceIds);
    }

    public List<Long> completedReviewWordIds() {
        return copyIds(completedReviewWordIds);
    }

    public boolean hasAssignedWord(Long wordId) {
        return copyIds(assignedWordIds).contains(wordId);
    }

    public boolean hasAssignedSentence(Long sentenceId) {
        return copyIds(assignedSentenceIds).contains(sentenceId);
    }

    public boolean hasAssignedReview(Long wordId) {
        return copyIds(assignedReviewWordIds).contains(wordId);
    }

    public boolean isSentenceCompleted(Long sentenceId) {
        return copyIds(completedSentenceIds).contains(sentenceId);
    }

    public boolean isReviewCompleted(Long wordId) {
        return copyIds(completedReviewWordIds).contains(wordId);
    }

    public boolean isReadyToComplete() {
        return wordsCompleted >= copyIds(assignedWordIds).size()
                && copyIds(completedSentenceIds).containsAll(copyIds(assignedSentenceIds))
                && copyIds(completedReviewWordIds).containsAll(copyIds(assignedReviewWordIds));
    }

    public void addWords(int count) {
        this.wordsCompleted += requirePositive(count);
    }

    public void addSentences(int count) {
        this.sentencesCompleted += requirePositive(count);
    }

    public void completeSentence(Long sentenceId) {
        if (isSentenceCompleted(sentenceId)) {
            return;
        }
        this.completedSentenceIds = appendId(completedSentenceIds, sentenceId);
        addSentences(1);
    }

    public void completeReview(Long wordId) {
        if (isReviewCompleted(wordId)) {
            return;
        }
        this.completedReviewWordIds = appendId(completedReviewWordIds, wordId);
    }

    public void markCompleted() {
        this.completed = true;
    }

    public void markGoldAwarded() {
        this.goldAwarded = true;
    }

    public void restoreStreak(int streakCount) {
        this.streakCount = streakCount;
    }

    private static int requirePositive(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("학습 완료 수는 1 이상씩만 누적할 수 있습니다: " + count);
        }
        return count;
    }

    private static List<Long> copyIds(List<?> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> copied = new ArrayList<>(ids.size());
        for (Object id : ids) {
            if (id instanceof Number number) {
                copied.add(number.longValue());
            }
        }
        return List.copyOf(copied);
    }

    private static List<Long> appendId(List<?> current, Long id) {
        List<Long> next = new ArrayList<>(copyIds(current));
        next.add(id);
        return List.copyOf(next);
    }
}
