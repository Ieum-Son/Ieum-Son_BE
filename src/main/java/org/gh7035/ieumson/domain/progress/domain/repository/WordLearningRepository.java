package org.gh7035.ieumson.domain.progress.domain.repository;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WordLearningRepository extends JpaRepository<WordLearning, Long> {
    Optional<WordLearning> findByMemberAndWord(Member member, Word word);

    @Query("select wl from WordLearning wl join fetch wl.word where wl.member = :member and wl.word.id in :wordIds")
    List<WordLearning> findByMemberAndWordIdIn(@Param("member") Member member, @Param("wordIds") Collection<Long> wordIds);

    @Query("select wl from WordLearning wl join fetch wl.word where wl.member = :member and wl.isMastered = true")
    List<WordLearning> findByMemberAndIsMasteredTrue(@Param("member") Member member);
}
