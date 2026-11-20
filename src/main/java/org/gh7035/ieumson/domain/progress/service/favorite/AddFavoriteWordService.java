package org.gh7035.ieumson.domain.progress.service.favorite;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.WordFavorite;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.repository.WordFavoriteRepository;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
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
public class AddFavoriteWordService {

    private final CurrentMemberFinder currentMemberFinder;
    private final WordRepository wordRepository;
    private final WordLearningRepository wordLearningRepository;
    private final WordFavoriteRepository wordFavoriteRepository;

    public void execute(CustomUserDetails userDetails, Long wordId) {
        Member member = currentMemberFinder.get(userDetails);
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IeumException(ErrorCode.WORD_NOT_FOUND));

        WordLearning learning = wordLearningRepository.findByMemberAndWord(member, word)
                .orElseThrow(() -> new IeumException(ErrorCode.WORD_NOT_MASTERED));
        if (!Boolean.TRUE.equals(learning.getIsMastered())) {
            throw new IeumException(ErrorCode.WORD_NOT_MASTERED);
        }
        if (wordFavoriteRepository.existsByMemberAndWord(member, word)) {
            throw new IeumException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        wordFavoriteRepository.save(WordFavorite.of(member, word));
    }
}
