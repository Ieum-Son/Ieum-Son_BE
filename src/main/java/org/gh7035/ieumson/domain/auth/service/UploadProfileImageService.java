package org.gh7035.ieumson.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.auth.presentation.dto.response.ProfileImageResponse;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.infrastructure.s3.S3FileUploader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UploadProfileImageService {

    private final MemberRepository memberRepository;
    private final S3FileUploader s3FileUploader;

    @Transactional
    public ProfileImageResponse execute(String loginId, MultipartFile image) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IeumException(ErrorCode.MEMBER_NOT_FOUND));

        String previousImageUrl = member.getProfileImageUrl();
        String uploadedUrl = s3FileUploader.uploadProfileImage(loginId, image);
        member.updateProfileImageUrl(uploadedUrl);
        s3FileUploader.deleteIfOwned(previousImageUrl);

        return new ProfileImageResponse(uploadedUrl);
    }
}
