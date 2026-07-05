package org.gh7035.ieumson.domain.auth.presentation.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
