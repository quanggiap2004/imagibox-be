package com.imagibox.service;

import com.imagibox.dto.response.DashboardResponseDto;
import com.imagibox.repository.ChapterRepository;
import com.imagibox.repository.MoodTagRepository;
import com.imagibox.repository.StoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public DashboardResponseDto getDashboard(Long userId) {
        log.info("Getting dashboard for user {}", userId);

        long totalStories = storyRepository.countByUserId(userId);

        // Stories this week
        OffsetDateTime oneWeekAgo = OffsetDateTime.now().minusWeeks(1);
        OffsetDateTime now = OffsetDateTime.now();
        long storiesThisWeek = storyRepository.findStoriesByUserAndDateRange(userId, oneWeekAgo, now).size();

        // Average chapters per story
        double avgChapters = calculateAvgChaptersPerStory(userId);

        Map<String, Long> moodDistribution = getMoodDistribution(userId);

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

    public Map<String, Long> getMoodDistribution(Long userId) {
        List<Map<String, Object>> results = moodTagRepository.getMoodDistributionByUserId(userId);

        return results.stream()
                .collect(Collectors.toMap(
                        result -> (String) result.get("mood"),
                        result -> ((Number) result.get("count")).longValue()));
    }

    private double calculateAvgChaptersPerStory(Long userId) {
        var stories = storyRepository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged());

        if (stories.isEmpty()) {
            return 0.0;
        }

        long totalChapters = stories.stream()
                .mapToLong(story -> chapterRepository.countByStoryId(story.getId()))
                .sum();

        return (double) totalChapters / stories.getTotalElements();
    }
}
