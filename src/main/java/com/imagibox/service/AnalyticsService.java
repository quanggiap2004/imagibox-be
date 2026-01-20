package com.imagibox.service;

import com.imagibox.dto.response.DashboardResponseDto;
import com.imagibox.repository.ChapterRepository;
import com.imagibox.repository.MoodTagRepository;
import com.imagibox.repository.StoryRepository;
import com.imagibox.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final MoodTagRepository moodTagRepository;
    private final UserRepository userRepository;

    public DashboardResponseDto getDashboard(Long parentId) {
        log.info("Getting dashboard for parent {}", parentId);

        List<Long> kidIds = userRepository.findKidIdByParentId(parentId);

        // If no kids, return empty dashboard
        if (kidIds.isEmpty()) {
            return DashboardResponseDto.builder()
                    .totalStories(0L)
                    .storiesThisWeek(0L)
                    .avgChaptersPerStory(0.0)
                    .moodDistribution(new HashMap<>())
                    .activitySummary(new HashMap<>())
                    .build();
        }

        long totalStories = storyRepository.countByUserIdIn(kidIds);

        // Stories this week
        OffsetDateTime oneWeekAgo = OffsetDateTime.now().minusWeeks(1);
        OffsetDateTime now = OffsetDateTime.now();
        long storiesThisWeek = storyRepository.findStoriesByUserAndDateRange(kidIds, oneWeekAgo, now).size();

        // Average chapters per story
        double avgChapters = calculateAvgChaptersPerStory(kidIds);

        Map<String, Long> moodDistribution = getMoodDistribution(parentId);

        Map<String, Object> activitySummary = new HashMap<>();
        activitySummary.put("totalStories", totalStories);
        activitySummary.put("storiesThisWeek", storiesThisWeek);

        return DashboardResponseDto.builder()
                .totalStories(totalStories)
                .storiesThisWeek(storiesThisWeek)
                .avgChaptersPerStory(avgChapters)
                .moodDistribution(moodDistribution)
                .activitySummary(activitySummary)
                .build();
    }

    public Map<String, Long> getMoodDistribution(Long parentId) {
        List<Long> kidIds = userRepository.findKidIdByParentId(parentId);

        if (kidIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Map<String, Object>> results = moodTagRepository.getMoodDistributionByUserIdIn(kidIds);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> (String) result.get("mood"),
                        result -> ((Number) result.get("count")).longValue()));
    }

    private double calculateAvgChaptersPerStory(List<Long> kidsIds) {
        var stories = storyRepository.findByUserIdIn(kidsIds, Pageable.unpaged());

        if (stories.isEmpty()) {
            return 0.0;
        }

        long totalChapters = stories.stream()
                .mapToLong(story -> chapterRepository.countByStoryId(story.getId()))
                .sum();

        return (double) totalChapters / stories.getTotalElements();
    }
}
