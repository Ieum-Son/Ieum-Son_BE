package org.gh7035.ieumson.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.domain.repository.MemberRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.infrastructure.mail.MailSenderService;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordReissueService {
    private final MailSenderService mailSender;
    private final MemberRepository memberRepository;

    public void execute(String loginId, String password) {
        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IeumException(ErrorCode.MEMBER_NOT_FOUND));





        try {
            mailSender.sendHtmlTemplate(
                    member.email(),
                    "[이음손] 비밀번호 재설정",
                    "templates/reissuePasswordTemplate.html",
                    Map.of("temporaryPassword", temporaryPassword)
            );
        } catch (MailException | IllegalStateException e) {
            throw new IeumException(ErrorCode.AUTH_MAIL_SEND_FAILED, e);
        }
    }
}
