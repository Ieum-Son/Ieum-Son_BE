package org.gh7035.ieumson.domain.auth.presentation;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.LoginRequest;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.RefreshRequest;
import org.gh7035.ieumson.domain.auth.presentation.dto.request.SignUpRequest;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.TokenResponse;
import org.gh7035.ieumson.domain.auth.service.AuthService;
import org.gh7035.ieumson.domain.auth.service.TokenService;
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

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@RequestBody @Valid SignUpRequest request) {
        authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
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
}
