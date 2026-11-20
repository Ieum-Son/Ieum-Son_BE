package org.gh7035.ieumson.domain.member.presentation.dto.request;

public record UpdateSettingsRequest(
        Boolean darkMode,
        Boolean alarmEnabled
) {
}
