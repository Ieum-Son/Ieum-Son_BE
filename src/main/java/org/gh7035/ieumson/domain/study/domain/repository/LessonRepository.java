package org.gh7035.ieumson.domain.study.domain.repository;

import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByChapterIdOrderByOrderNumberAsc(Long chapterId);

    @Query("select l from Lesson l join fetch l.chapter where l.id = :lessonId")
    Optional<Lesson> findWithChapterById(@Param("lessonId") Long lessonId);
}
