package org.gh7035.ieumson.domain.member.presentation.dto.response;

public record SettingsResponse(
        boolean darkMode,
        boolean alarmEnabled
) {
}
