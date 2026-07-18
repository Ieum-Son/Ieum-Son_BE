package org.gh7035.ieumson.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.domain.RefreshToken;
import org.gh7035.ieumson.domain.auth.domain.repository.RefreshTokenRepository;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.TokenResponse;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.error.exception.TokenException;
import org.gh7035.ieumson.global.security.jwt.JwtProperties;
import org.gh7035.ieumson.global.security.jwt.JwtTokenProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public TokenResponse issueTokens(String loginId) {
        String accessToken = jwtTokenProvider.generateAccessToken(loginId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(loginId);

        refreshTokenRepository.save(RefreshToken.builder()
                .loginId(loginId)
                .refreshToken(refreshToken)
                .expireTime(jwtProperties.getRefreshTokenExpiry())
                .build());

        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse refresh(String refreshToken) {
        String loginId = jwtTokenProvider.getLoginIdFromRefreshToken(refreshToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(loginId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(loginId);
        long ttlMs = jwtProperties.getRefreshTokenExpiry();

        int result = refreshTokenRepository.rotateIfMatches(
                loginId, refreshToken, newRefreshToken, ttlMs);

        if (result == -1) {
            throw new IeumException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        if (result == 0) {
            throw new TokenException(ErrorCode.INVALID_TOKEN);
        }

        return new TokenResponse(newAccessToken, newRefreshToken);
    }


}
