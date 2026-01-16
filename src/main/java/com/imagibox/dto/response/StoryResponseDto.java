package com.imagibox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoryResponseDto {

    private Long id;
    private String title;
    private String status;
    private String mode;
    private Map<String, Object> metadata;
    private OffsetDateTime createdAt;
    private List<ChapterResponseDto> chapters;
}
