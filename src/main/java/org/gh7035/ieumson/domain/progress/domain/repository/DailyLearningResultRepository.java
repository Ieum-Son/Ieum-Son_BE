package org.gh7035.ieumson.domain.progress.domain.repository;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLearningResultRepository extends JpaRepository<DailyLearningResult, Long> {
    Optional<DailyLearningResult> findByMemberAndLearnedDate(Member member, LocalDate learnedDate);

    @Modifying(flushAutomatically = true)
    @Query("update DailyLearningResult d set d.goldAwarded = true where d.id = :id and d.goldAwarded = false")
    int claimGoldAward(@Param("id") Long id);

    @Query("select max(d.streakCount) from DailyLearningResult d where d.member = :member")
    Optional<Integer> findLongestStreakByMember(@Param("member") Member member);

    @Query("select count(d) from DailyLearningResult d " +
            "where d.member = :member " +
            "and d.learnedDate between :monthStartDay and :today " +
            "and d.completed = true")
    int countCompletedByMemberBetween(@Param("member") Member member, @Param("monthStartDay") LocalDate MonthStartDay, @Param("today") LocalDate today);

    @Query("select count(d) from DailyLearningResult d where d.member = :member and d.completed = true")
    int countCompletedByMember(@Param("member") Member member);

    Optional<DailyLearningResult> findTopByMemberOrderByLearnedDateDesc(Member member);

    Optional<DailyLearningResult> findTopByMemberAndLearnedDateBeforeOrderByLearnedDateDesc(Member member, LocalDate date);

    List<DailyLearningResult> findByMemberAndLearnedDateBetween(Member member, LocalDate from, LocalDate to);
}
