package org.gh7035.ieumson.domain.member.domain.repository;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByLoginId(String loginId);
}
