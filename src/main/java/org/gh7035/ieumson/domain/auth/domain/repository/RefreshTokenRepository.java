package org.gh7035.ieumson.domain.auth.domain.repository;

import org.gh7035.ieumson.domain.auth.domain.RefreshToken;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByEmail(String accountId);

}
