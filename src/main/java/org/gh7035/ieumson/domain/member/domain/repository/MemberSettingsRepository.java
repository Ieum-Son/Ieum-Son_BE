package org.gh7035.ieumson.domain.member.domain.repository;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.MemberSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberSettingsRepository extends JpaRepository<MemberSettings, Long> {
    Optional<MemberSettings> findByMember(Member member);
}
