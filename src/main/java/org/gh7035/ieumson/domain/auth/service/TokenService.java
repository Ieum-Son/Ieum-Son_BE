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

    public TokenResponse issueTokens(String email) {
        String accessToken = jwtTokenProvider.generateAccessToken(email);
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);

        refreshTokenRepository.save(RefreshToken.builder()
                .email(email)
                .refreshToken(refreshToken)
                .expireTime(jwtProperties.getRefreshTokenExpiry())
                .build());

        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse refresh(String refreshToken) {
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        RefreshToken stored = refreshTokenRepository.findById(email)
                .orElseThrow(() -> new IeumException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (!stored.getRefreshToken().equals(refreshToken)) {
            throw new TokenException(ErrorCode.INVALID_TOKEN);
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        stored.rotateToken(newRefreshToken, jwtProperties.getRefreshTokenExpiry());
        refreshTokenRepository.save(stored);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String email) {
        refreshTokenRepository.deleteById(email);
    }
}
