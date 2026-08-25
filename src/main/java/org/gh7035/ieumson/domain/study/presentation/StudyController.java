package org.gh7035.ieumson.domain.study.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.study.presentation.dto.response.CurriculumResponse;
import org.gh7035.ieumson.domain.study.service.GetCurriculumService;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Study", description = "학습 로드맵 API")
@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyController {

    private final GetCurriculumService getCurriculumService;

    @Operation(summary = "학습 로드맵 조회", description = "챕터/레슨 목록과 현재 챕터, 레슨별 진도. FUNC-002-03, FUNC-002-04")
    @GetMapping("/chapters")
    @ResponseStatus(HttpStatus.OK)
    public CurriculumResponse getChapters(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return getCurriculumService.execute(userDetails);
    }
}
