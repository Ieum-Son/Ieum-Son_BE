package org.gh7035.ieumson.domain.progress.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.WordFavorite;
import org.gh7035.ieumson.domain.progress.domain.repository.WordFavoriteRepository;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.FavoriteWordListResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.FavoriteWordListResponse.FavoriteWordResponse;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFavoriteWordsService {

    private final CurrentMemberFinder currentMemberFinder;
    private final WordFavoriteRepository wordFavoriteRepository;

    public FavoriteWordListResponse execute(CustomUserDetails userDetails) {
        Member member = currentMemberFinder.get(userDetails);
        return new FavoriteWordListResponse(
                wordFavoriteRepository.findByMemberOrderByCreatedAtDesc(member).stream()
                        .map(WordFavorite::getWord)
                        .map(word -> new FavoriteWordResponse(
                                word.getId(),
                                word.getWord(),
                                word.getDefinition(),
                                word.getVideoLink(),
                                word.getCategory()
                        ))
                        .toList()
        );
    }
}
