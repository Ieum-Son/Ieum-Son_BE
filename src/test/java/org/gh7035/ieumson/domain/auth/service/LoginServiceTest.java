package org.gh7035.ieumson.domain.auth.service;

import org.gh7035.ieumson.domain.auth.presentation.dto.request.LoginRequest;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.TokenResponse;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.enums.Role;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.ErrorResponse;
import org.gh7035.ieumson.global.error.GlobalExceptionHandler;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.infrastructure.redis.RedisRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private static final String LOGIN_ID = "dino";
    private static final String EMAIL = "dino@gmail.com";
    private static final String RAW_PASSWORD = "!@12QWqw";

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final TokenService tokenService = mock(TokenService.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final RedisRateLimiter redisRateLimiter = mock(RedisRateLimiter.class);
    private final LoginService loginService = new LoginService(
            memberRepository, tokenService, passwordEncoder, redisRateLimiter);

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email(EMAIL)
                .name("홍길동")
                .loginId(LOGIN_ID)
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .emailVerified(true)
                .role(Role.USER)
                .build();
    }

    @Test
    void loginFailedExceptionMatches400Response() {
        IeumException exception = new IeumException(ErrorCode.LOGIN_FAILED);

        ResponseEntity<ErrorResponse> response = new GlobalExceptionHandler()
                .handleIeumException(exception);

        assertThat(exception.getErrorCode().getStatusCode()).isEqualTo(400);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("로그인에 실패했습니다.");
    }

    @Test
    void loginSucceedsWithLoginIdAndPassword() {
        when(memberRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.of(member));
        when(tokenService.issueTokens(LOGIN_ID)).thenReturn(new TokenResponse("access", "refresh"));

        TokenResponse response = loginService.execute(
                new LoginRequest(LOGIN_ID, RAW_PASSWORD), "127.0.0.1");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        verify(memberRepository, never()).findByEmail(LOGIN_ID);
    }

    @Test
    void loginFailsWhenLoginIdDoesNotExist() {
        when(memberRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(LOGIN_ID)).thenReturn(Optional.empty());

        assertLoginFailed(new LoginRequest(LOGIN_ID, RAW_PASSWORD));
    }

    @Test
    void loginSucceedsWhenEmailIsSentAsLoginId() {
        when(memberRepository.findByLoginId(EMAIL)).thenReturn(Optional.empty());
        when(memberRepository.findByEmail(EMAIL)).thenReturn(Optional.of(member));
        when(tokenService.issueTokens(LOGIN_ID)).thenReturn(new TokenResponse("access", "refresh"));

        TokenResponse response = loginService.execute(
                new LoginRequest(EMAIL, RAW_PASSWORD), "127.0.0.1");

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void loginFailsWhenPasswordDoesNotMatch() {
        when(memberRepository.findByLoginId(LOGIN_ID)).thenReturn(Optional.of(member));

        assertLoginFailed(new LoginRequest(LOGIN_ID, "wrong-password"));
    }

    private void assertLoginFailed(LoginRequest request) {
        assertThatThrownBy(() -> loginService.execute(request, "127.0.0.1"))
                .isInstanceOf(IeumException.class)
                .satisfies(thrown -> {
                    IeumException exception = (IeumException) thrown;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAILED);
                    assertThat(exception.getErrorCode().getStatusCode()).isEqualTo(400);
                    assertThat(exception.getErrorCode().getErrorMessage()).isEqualTo("로그인에 실패했습니다.");
                });
    }
}
