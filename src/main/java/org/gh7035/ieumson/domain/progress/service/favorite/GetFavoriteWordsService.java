package org.gh7035.ieumson.domain.progress.service.favorite;

import lombok.RequiredArgsConstructor;
import org.gh7035.ieumson.domain.member.domain.Member;
import org.gh7035.ieumson.domain.member.service.CurrentMemberFinder;
import org.gh7035.ieumson.domain.progress.domain.WordFavorite;
import org.gh7035.ieumson.domain.progress.domain.WordLearning;
import org.gh7035.ieumson.domain.progress.domain.repository.WordFavoriteRepository;
import org.gh7035.ieumson.domain.progress.domain.repository.WordLearningRepository;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.FavoriteWordListResponse;
import org.gh7035.ieumson.domain.progress.presentation.dto.response.FavoriteWordListResponse.FavoriteWordResponse;
import org.gh7035.ieumson.global.security.auth.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetFavoriteWordsService {

    private final CurrentMemberFinder currentMemberFinder;
    private final WordFavoriteRepository wordFavoriteRepository;
    private final WordLearningRepository wordLearningRepository;

    public FavoriteWordListResponse execute(CustomUserDetails userDetails) {
        Member member = currentMemberFinder.get(userDetails);
        List<WordFavorite> favoriteWords = wordFavoriteRepository.findByMemberOrderByCreatedAtDesc(member);

        List<Long> wordsIds = favoriteWords.stream()
                .map(f -> f.getWord().getId())
                .toList();

        Map<Long, LocalDateTime> learnedTimes = wordLearningRepository
                .findByMemberAndWordIdIn(member, wordsIds)
                .stream()
                .collect(Collectors.toMap(
                        wl -> wl.getWord().getId(), WordLearning::getLastPracticedAt
                ));

        return new FavoriteWordListResponse(
                favoriteWords.stream()
                        .map(WordFavorite::getWord)
                        .map(word -> new FavoriteWordResponse(
                                word.getId(),
                                word.getWord(),
                                learnedTimes.get(word.getId()),
                                word.getVideoLink(),
                                word.getCategory()
                        ))
                        .toList()
        );
    }
}
