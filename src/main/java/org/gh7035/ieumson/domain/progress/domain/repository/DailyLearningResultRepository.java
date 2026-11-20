package org.gh7035.ieumson.domain.progress.domain.repository;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.DailyLearningResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyLearningResultRepository extends JpaRepository<DailyLearningResult, Long> {
    Optional<DailyLearningResult> findByMemberAndLearnedDate(Member member, LocalDate learnedDate);

    Optional<DailyLearningResult> findTopByMemberOrderByLearnedDateDesc(Member member);

    Optional<DailyLearningResult> findTopByMemberAndLearnedDateBeforeOrderByLearnedDateDesc(Member member, LocalDate date);

    List<DailyLearningResult> findByMemberAndLearnedDateBetween(Member member, LocalDate from, LocalDate to);
}
