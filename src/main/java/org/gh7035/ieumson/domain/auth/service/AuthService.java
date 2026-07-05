package org.gh7035.ieumson.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.domain.RefreshToken;
import org.gh7035.ieumson.domain.auth.domain.repository.RefreshTokenRepository;
import org.gh7035.ieumson.domain.auth.dto.request.LoginRequest;
import org.gh7035.ieumson.domain.auth.dto.request.SignUpRequest;
import org.gh7035.ieumson.domain.auth.dto.response.TokenResponse;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.enums.Role;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.error.exception.TokenException;
import org.gh7035.ieumson.global.security.jwt.JwtProperties;
import org.gh7035.ieumson.global.security.jwt.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;

    public void signUp(SignUpRequest request) {
        if (memberRepository.findByEmail(request.email()).isPresent()) {
            throw new IeumException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .role(Role.USER)
                .build();

        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new IeumException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IeumException(ErrorCode.PASSWORD_MISMATCH);
        }

        if (!member.isEmailVerified()) {
            throw new IeumException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        return issueTokens(member.getEmail());
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

    private TokenResponse issueTokens(String email) {
        String accessToken = jwtTokenProvider.generateAccessToken(email);
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);

        refreshTokenRepository.save(RefreshToken.builder()
                .email(email)
                .refreshToken(refreshToken)
                .expireTime(jwtProperties.getRefreshTokenExpiry())
                .build());

        return new TokenResponse(accessToken, refreshToken);
    }
}
