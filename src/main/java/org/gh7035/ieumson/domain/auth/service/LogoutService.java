package org.gh7035.ieumson.domain.auth.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.domain.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void execute(String email) {
        refreshTokenRepository.deleteById(email);
    }
}
