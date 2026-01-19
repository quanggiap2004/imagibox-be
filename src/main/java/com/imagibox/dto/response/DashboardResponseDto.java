package com.imagibox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDto {

    private Long totalStories;
    private Long storiesThisWeek;
    private Double avgChaptersPerStory;
    private Map<String, Long> moodDistribution;
    private Map<String, Object> activitySummary;
}
