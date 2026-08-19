package org.gh7035.ieumson.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.global.entity.BaseEntity;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "member_settings")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberSettings extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder.Default
    @Column(name = "dark_mode")
    private boolean darkMode = false;

    @Builder.Default
    @Column(name = "alarm_enabled")
    private boolean alarmEnabled = true;

    public void toggleDarkMode() { this.darkMode = !this.darkMode; }

    public void toggleAlramEnabled() { this.alarmEnabled = !this.alarmEnabled; }

}
