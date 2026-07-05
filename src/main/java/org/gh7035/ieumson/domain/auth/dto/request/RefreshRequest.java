package org.gh7035.ieumson.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "refreshToken을 입력해주세요.")
        String refreshToken
) {
}
