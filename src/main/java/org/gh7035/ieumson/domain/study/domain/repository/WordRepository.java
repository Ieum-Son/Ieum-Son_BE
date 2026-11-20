package org.gh7035.ieumson.domain.study.domain.repository;

import org.gh7035.ieumson.domain.study.domain.Lesson;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {

    @Query("""
            select w from Word w
            join fetch w.lesson l
            join fetch l.chapter c
            order by c.orderNumber, l.orderNumber, w.id
            """)
    List<Word> findAllInCurriculumOrder();

    List<Word> findByIdIn(Collection<Long> ids);

    List<Word> findByLessonIn(Collection<Lesson> lessons);
}
