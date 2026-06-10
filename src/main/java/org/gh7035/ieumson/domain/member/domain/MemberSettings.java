package org.gh7035.ieumson.domain.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import org.gh7035.ieumson.global.entity.BaseEntity;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.Date;

@Entity

public class MemberSettings extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "dark_mode")
    private boolean darkMode = false;

    @Column(name = "alram_enabled")
    private boolean alramEnabled = true;

//    @Column(name = "sound_volume")
//    private int soundVolume;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void toggleDarkMode() { this.darkMode = !this.darkMode; }

    public void toggleAlramEnabled() { this.alramEnabled = !this.alramEnabled; }

//    public void SoundVolume(int soundVolume) { this.soundVolume = soundVolume; }
}
