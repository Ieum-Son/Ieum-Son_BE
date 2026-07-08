package org.gh7035.ieumson.domain.auth.presentation;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.*;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.TokenResponse;
import org.gh7035.ieumson.domain.auth.service.*;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final SignupService signupService;
    private final VerifyEmailService verifyEmailService;
    private final VerifyCodeService verifyCodeService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@RequestBody @Valid SignUpRequest request) {
        signupService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyEmail (@RequestBody @Valid VerifyRequest request, HttpServletRequest httpRequest) {
        verifyEmailService.verifyEmail(request, extractClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/code")
    public ResponseEntity<Void> verifyCode (@RequestBody @Valid VerifyCodeRequest request, HttpServletRequest httpRequest) {
        verifyCodeService.verifyCode(request, extractClientIp(httpRequest));
        return ResponseEntity.ok().build();
    }



    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshRequest request) {
        return ResponseEntity.ok(tokenService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        tokenService.logout(userDetails.getUsername());
        return ResponseEntity.noContent().build();
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
