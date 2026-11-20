package org.gh7035.ieumson.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.presentation.dto.request.UpdateNameRequest;
import org.gh7035.ieumson.domain.member.presentation.dto.response.ProfileResponse;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateNameService {

    private final CurrentMemberFinder currentMemberFinder;
    private final GetProfileService getProfileService;

    public ProfileResponse execute(CustomUserDetails userDetails, UpdateNameRequest request) {
        Member member = currentMemberFinder.get(userDetails);
        member.changeName(request.name());
        return getProfileService.execute(userDetails);
    }
}
