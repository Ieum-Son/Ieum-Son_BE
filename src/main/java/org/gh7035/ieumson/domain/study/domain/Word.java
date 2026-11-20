package org.gh7035.ieumson.domain.study.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.global.entity.BaseEntity;

@Entity
@Table(name = "word")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Word extends BaseEntity {

    @Column(nullable = false)
    private String word;

    @Column(nullable = false)
    private String definition;

    @Column(nullable = false)
    private String videoLink;

    @Column(nullable = false)
    private String category;

    @Builder.Default
    @Column(nullable = false)
    private int difficulty = 1;

    @ManyToOne
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;


}
