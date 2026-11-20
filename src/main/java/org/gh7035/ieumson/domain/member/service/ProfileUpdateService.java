package org.gh7035.ieumson.domain.member.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.ProfileImageResponse;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.gh7035.ieumson.infrastructure.s3.S3FileManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileUpdateService {

    private final S3FileManager s3FileManager;
    private final CurrentMemberFinder currentMemberFinder;
    private final MemberRepository memberRepository;


    public ProfileImageResponse execute(CustomUserDetails userDetails, MultipartFile newImage) {
        Member member = currentMemberFinder.get(userDetails);
        String oldImageUrl = member.getProfileImageUrl();
        String currentProfileImageUrl = s3FileManager.uploadProfileImage(member.getLoginId(), newImage);

        try {
            member.updateProfileImageUrl(currentProfileImageUrl);
            memberRepository.saveAndFlush(member);

        } catch (DataIntegrityViolationException e) {
            s3FileManager.deleteIfOwned(currentProfileImageUrl);
            throw new IeumException(ErrorCode.DUPLICATE_ENTRY, e);

        } catch (RuntimeException e){
            s3FileManager.deleteIfOwned(currentProfileImageUrl);
            throw e;

        }

        s3FileManager.deleteIfOwned(oldImageUrl);
        return new ProfileImageResponse(currentProfileImageUrl);

    }

}
