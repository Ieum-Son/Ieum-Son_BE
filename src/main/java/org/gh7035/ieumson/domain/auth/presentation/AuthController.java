package org.gh7035.ieumson.domain.auth.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.*;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.ProfileImageResponse;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.TokenResponse;
import org.gh7035.ieumson.domain.auth.service.*;
import org.gh7035.ieumson.domain.member.presentation.dto.request.UpdateNameRequest;
import org.gh7035.ieumson.domain.member.presentation.dto.response.ProfileResponse;
import org.gh7035.ieumson.domain.member.service.UpdateNameService;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Auth", description = "회원가입·로그인·토큰 API. FUNC-008, FUNC-003")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final TokenService tokenService;
    private final SignupService signupService;
    private final VerifyEmailService verifyEmailService;
    private final VerifyCodeService verifyCodeService;
    private final LogoutService logoutService;
    private final DeleteAccountService deleteAccountService;
    private final UpdateNameService updateNameService;

    @Operation(summary = "회원가입", description = "이메일 인증 완료 후 프로필 이미지와 함께 가입한다. FUNC-008, FUNT-008-01")
    @SecurityRequirements
    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileImageResponse signUp(
            @Valid @ModelAttribute SignUpRequest request,
            @RequestParam("image") MultipartFile image
    ) {
        return signupService.execute(request, image);
    }

    @Operation(summary = "이메일 인증 코드 발송", description = "FUNC-008")
    @SecurityRequirements
    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.OK)
    public void verifyEmail (@RequestBody @Valid VerifyRequest request, HttpServletRequest httpRequest) {
        verifyEmailService.execute(request, extractClientIp(httpRequest));
    }

    @Operation(summary = "이메일 인증 코드 확인", description = "FUNC-008")
    @SecurityRequirements
    @PostMapping("/code")
    @ResponseStatus(HttpStatus.OK)
    public void verifyCode (@RequestBody @Valid VerifyCodeRequest request, HttpServletRequest httpRequest) {
        verifyCodeService.execute(request, extractClientIp(httpRequest));
    }

    @Operation(summary = "로그인", description = "FUNC-003-01")
    @SecurityRequirements
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        return loginService.execute(request, extractClientIp(httpRequest));
    }

    @Operation(summary = "토큰 재발급", description = "Refresh Token rotation")
    @SecurityRequirements
    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse refresh(@RequestBody @Valid RefreshRequest request) {
        return tokenService.refresh(request.refreshToken());
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        logoutService.execute(userDetails.getUsername());
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid DeleteAccountRequest request
    ) {
        deleteAccountService.execute(userDetails, request);
    }

    @Operation(summary = "이름 재설정", description = "FUNC-007-02")
    @PatchMapping("/rename")
    @ResponseStatus(HttpStatus.OK)
    public ProfileResponse rename(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UpdateNameRequest request
    ) {
        return updateNameService.execute(userDetails, request);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
