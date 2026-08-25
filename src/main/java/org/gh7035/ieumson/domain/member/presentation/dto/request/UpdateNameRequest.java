package org.gh7035.ieumson.domain.member.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNameRequest(
        @NotBlank(message = "이름을 입력해주세요.")
        @Size(max = 255, message = "이름은 255자 이하여야 합니다.")
        String name
) {
}
