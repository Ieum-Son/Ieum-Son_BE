package org.gh7035.ieumson.domain.member.domain;

import jakarta.persistence.*;
import lombok.*;
import org.gh7035.ieumson.global.entity.BaseEntity;

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

    public void update(Boolean darkMode, Boolean alarmEnabled) {
        if (darkMode != null) {
            this.darkMode = darkMode;
        }
        if (alarmEnabled != null) {
            this.alarmEnabled = alarmEnabled;
        }
    }

    public static MemberSettings of(Member member) {
        return MemberSettings.builder()
                .member(member)
                .darkMode(false)
                .alarmEnabled(true)
                .build();
    }

}
