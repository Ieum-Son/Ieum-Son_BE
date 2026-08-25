package org.gh7035.ieumson.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.MemberSettings;
import org.gh7035.ieumson.domain.member.domain.repository.MemberSettingsRepository;
import org.gh7035.ieumson.domain.member.presentation.dto.request.UpdateSettingsRequest;
import org.gh7035.ieumson.domain.member.presentation.dto.response.SettingsResponse;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberSettingsService {

    private final CurrentMemberFinder currentMemberFinder;
    private final MemberSettingsRepository memberSettingsRepository;

    @Transactional(readOnly = true)
    public SettingsResponse get(CustomUserDetails userDetails) {
        Member member = currentMemberFinder.get(userDetails);
        MemberSettings settings = memberSettingsRepository.findByMember(member)
                .orElseGet(() -> MemberSettings.of(member));
        return toResponse(settings);
    }

    public SettingsResponse update(CustomUserDetails userDetails, UpdateSettingsRequest request) {
        Member member = currentMemberFinder.get(userDetails);
        MemberSettings settings = memberSettingsRepository.findByMember(member)
                .orElseGet(() -> memberSettingsRepository.save(MemberSettings.of(member)));
        settings.update(request.darkMode(), request.alarmEnabled());
        return toResponse(settings);
    }

    private SettingsResponse toResponse(MemberSettings settings) {
        return new SettingsResponse(settings.isDarkMode(), settings.isAlarmEnabled());
    }
}
