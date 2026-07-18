package org.gh7035.ieumson.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.domain.member.domain.enums.Role;
import org.gh7035.ieumson.global.entity.BaseEntity;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@Builder
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

    @Column(name = "nugget_balance")
    private int nuggetBalance = 0;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

}
