package org.gh7035.ieumson.domain.study.domain.repository;

import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.gh7035.ieumson.domain.study.domain.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {

    @Query("""
            select s from Sentence s
            join fetch s.blankWord
            join fetch s.lesson
            where s.lesson in :lessons
            order by s.orderNum, s.id
            """)
    List<Sentence> findByLessonInOrderByOrderNumAsc(@Param("lessons") Collection<Lesson> lessons);

    List<Sentence> findByIdIn(Collection<Long> ids);
}
