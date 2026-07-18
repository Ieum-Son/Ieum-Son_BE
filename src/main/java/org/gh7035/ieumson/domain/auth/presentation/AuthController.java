package org.gh7035.ieumson.domain.auth.presentation;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.*;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.ProfileImageResponse;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.TokenResponse;
import org.gh7035.ieumson.domain.auth.service.*;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final UploadProfileImageService uploadProfileImageService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signUp(@RequestBody @Valid SignUpRequest request) { signupService.execute(request); }

    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.OK)
    public void verifyEmail (@RequestBody @Valid VerifyRequest request, HttpServletRequest httpRequest) {
        verifyEmailService.execute(request, extractClientIp(httpRequest));
    }

    @PostMapping("/code")
    @ResponseStatus(HttpStatus.OK)
    public void verifyCode (@RequestBody @Valid VerifyCodeRequest request, HttpServletRequest httpRequest) {
        verifyCodeService.execute(request, extractClientIp(httpRequest));
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        return loginService.execute(request, extractClientIp(httpRequest));
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public TokenResponse refresh(@RequestBody @Valid RefreshRequest request) {
        return tokenService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        logoutService.execute(userDetails.getUsername());
    }

    @PostMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ProfileImageResponse uploadProfileImage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("image") MultipartFile image
    ) {
        return uploadProfileImageService.execute(userDetails.getUsername(), image);
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
