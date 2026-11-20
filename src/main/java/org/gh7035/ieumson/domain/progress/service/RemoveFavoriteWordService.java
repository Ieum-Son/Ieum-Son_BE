package org.gh7035.ieumson.domain.progress.service;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.WordFavorite;
import org.gh7035.ieumson.domain.progress.domain.repository.WordFavoriteRepository;
import org.gh7035.ieumson.domain.study.domain.Word;
import org.gh7035.ieumson.domain.study.domain.repository.WordRepository;
import org.gh7035.ieumson.global.error.exception.ErrorCode;
import org.gh7035.ieumson.global.error.exception.IeumException;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RemoveFavoriteWordService {

    private final CurrentMemberFinder currentMemberFinder;
    private final WordRepository wordRepository;
    private final WordFavoriteRepository wordFavoriteRepository;

    public void execute(CustomUserDetails userDetails, Long wordId) {
        Member member = currentMemberFinder.get(userDetails);
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IeumException(ErrorCode.WORD_NOT_FOUND));
        WordFavorite favorite = wordFavoriteRepository.findByMemberAndWord(member, word)
                .orElseThrow(() -> new IeumException(ErrorCode.FAVORITE_NOT_FOUND));
        wordFavoriteRepository.delete(favorite);
    }
}
