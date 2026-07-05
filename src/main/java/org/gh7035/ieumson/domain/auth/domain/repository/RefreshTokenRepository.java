package org.gh7035.ieumson.domain.auth.domain.repository;

import org.gh7035.ieumson.domain.auth.domain.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
}
