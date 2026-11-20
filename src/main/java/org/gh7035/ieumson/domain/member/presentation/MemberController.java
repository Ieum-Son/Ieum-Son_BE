package org.gh7035.ieumson.domain.member.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.ProfileImageResponse;
import org.gh7035.ieumson.domain.member.presentation.dto.request.UpdateNameRequest;
import org.gh7035.ieumson.domain.member.presentation.dto.request.UpdateSettingsRequest;
import org.gh7035.ieumson.domain.member.presentation.dto.response.ProfileResponse;
import org.gh7035.ieumson.domain.member.presentation.dto.response.SettingsResponse;
import org.gh7035.ieumson.domain.member.service.GetProfileService;
import org.gh7035.ieumson.domain.member.service.MemberSettingsService;
import org.gh7035.ieumson.domain.member.service.ProfileUpdateService;
import org.gh7035.ieumson.domain.member.service.UpdateNameService;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Member", description = "마이페이지·설정 API")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final GetProfileService getProfileService;
    private final MemberSettingsService memberSettingsService;
    private final UpdateNameService updateNameService;
    private final ProfileUpdateService profileUpdateService;

    @Operation(summary = "내 정보 조회", description = "프로필 사진, 이름, 이메일, 금조각, 스트릭. FUNC-007-09")
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public ProfileResponse getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return getProfileService.execute(userDetails);
    }

    @Operation(summary = "이름 재설정", description = "FUNC-007-02")
    @PatchMapping("/me/rename")
    @ResponseStatus(HttpStatus.OK)
    public ProfileResponse rename(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UpdateNameRequest request
    ) {
        return updateNameService.execute(userDetails, request);
    }

    @Operation(summary = "설정 조회", description = "다크모드, 학습 알림. FUNC-007-01, FUNC-007-04")
    @GetMapping("/me/settings")
    @ResponseStatus(HttpStatus.OK)
    public SettingsResponse getSettings(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return memberSettingsService.get(userDetails);
    }

    @Operation(summary = "설정 변경", description = "전달한 필드만 변경한다. FUNC-007-01, FUNC-007-04")
    @PatchMapping("/me/settings")
    @ResponseStatus(HttpStatus.OK)
    public SettingsResponse updateSettings(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid UpdateSettingsRequest request
    ) {
        return memberSettingsService.update(userDetails, request);
    }

    @Operation(summary = "프로필 이미지 재설정")
    @PatchMapping(path = "/me/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public ProfileImageResponse updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("image") MultipartFile image
    ) {
        return profileUpdateService.execute(userDetails, image);
    }
}
