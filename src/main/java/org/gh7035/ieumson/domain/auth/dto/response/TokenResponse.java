package org.gh7035.ieumson.domain.auth.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
