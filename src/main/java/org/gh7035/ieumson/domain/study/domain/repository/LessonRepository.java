package org.gh7035.ieumson.domain.study.domain.repository;

import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {
    List<Lesson> findByChapterIdOrderByOrderNumberAsc(Long chapterId);
}
