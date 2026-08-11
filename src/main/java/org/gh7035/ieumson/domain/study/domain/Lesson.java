package org.gh7035.ieumson.domain.study.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.global.entity.BaseEntity;

@Entity
@Builder
@Table(name = "lesson")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Lesson extends BaseEntity {

    @Column(name = "lesson_name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false)
    private Integer orderNumber;

    @Column(nullable = false)
    private String description;

}
