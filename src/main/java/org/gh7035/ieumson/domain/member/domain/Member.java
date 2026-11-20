package org.gh7035.ieumson.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.domain.member.domain.enums.Role;
import org.gh7035.ieumson.global.entity.BaseEntity;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@Builder
@SQLRestriction("deleted_at IS NULL") // 일반 조회시, deletedAt이 null인(탈퇴회원이 아닌) 회원만 조회
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Member extends BaseEntity {

    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;

    @Column(name = "login_id", length = 64, nullable = false, unique = true)
    private String loginId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Builder.Default
    @Column(name = "nugget_balance")
    private int nuggetBalance = 0;

    @Builder.Default
    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Builder.Default
    @Column(name = "deleted_at")
    LocalDateTime deletedAt = null;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void addGold(int amount) {
        this.nuggetBalance += amount;
    }

    public void deductGold(int amount) {
        this.nuggetBalance -= amount;
    }

    public void leave() {
        String anonymized = "deleted_" + this.getId();
        this.email = anonymized + "@deleted.local";
        this.loginId = anonymized;
        this.name = "탈퇴회원";
        this.password = "";
        this.profileImageUrl = null;
        this.nuggetBalance = 0;
        this.emailVerified = false;
        this.deletedAt = LocalDateTime.now();
    }

}
