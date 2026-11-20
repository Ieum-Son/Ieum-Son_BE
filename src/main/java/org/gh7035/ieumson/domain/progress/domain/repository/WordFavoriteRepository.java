package org.gh7035.ieumson.domain.progress.domain.repository;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.WordFavorite;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WordFavoriteRepository extends JpaRepository<WordFavorite, Long> {
    boolean existsByMemberAndWord(Member member, Word word);

    Optional<WordFavorite> findByMemberAndWord(Member member, Word word);

    @Query("select f from WordFavorite f join fetch f.word where f.member = :member order by f.createdAt desc")
    List<WordFavorite> findByMemberOrderByCreatedAtDesc(@Param("member") Member member);
}
