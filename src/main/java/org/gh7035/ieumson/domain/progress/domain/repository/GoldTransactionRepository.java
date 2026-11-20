package org.gh7035.ieumson.domain.progress.domain.repository;

import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.progress.domain.GoldTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoldTransactionRepository extends JpaRepository<GoldTransaction, Long> {
    Page<GoldTransaction> findByMemberOrderByCreatedAtDesc(Member member, Pageable pageable);
}
