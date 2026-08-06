package org.gh7035.ieumson.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.LoginRequest;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.TokenResponse;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.infrastructure.redis.RedisRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService {

    private final MemberRepository memberRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final RedisRateLimiter redisRateLimiter;

    private static final String LOGIN_RATE_IP_KEY_PREFIX = "auth:login:rate:ip:";
    private static final String LOGIN_RATE_LOGIN_ID_KEY_PREFIX = "auth:login:rate:loginId:";

    @Value("${app.login.max-requests-per-ip:20}")
    private long maxRequestsPerIp;

    @Value("${app.login.max-requests-per-login-id:10}")
    private long maxRequestsPerLoginId;

    @Value("${app.login.rate-limit-window-seconds:300}")
    private long rateLimitWindowSeconds;

    @Transactional(readOnly = true)
    public TokenResponse execute(LoginRequest request, String clientIp) {
        redisRateLimiter.check(
                LOGIN_RATE_LOGIN_ID_KEY_PREFIX + request.loginId(),
                maxRequestsPerLoginId,
                rateLimitWindowSeconds
        );
        if (clientIp != null && !clientIp.isBlank()) {
            redisRateLimiter.check(
                    LOGIN_RATE_IP_KEY_PREFIX + clientIp,
                    maxRequestsPerIp,
                    rateLimitWindowSeconds
            );
        }

        Member member = memberRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new IeumException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new IeumException(ErrorCode.LOGIN_FAILED);
        }

        if (!member.isEmailVerified()) {
            throw new IeumException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        return tokenService.issueTokens(member.getLoginId());
    }
}
