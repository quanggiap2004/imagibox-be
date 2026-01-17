package com.imagibox.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterResponseDto {

    private Long id;
    private Integer chapterNumber;
    private Map<String, Object> content;
    private String imageUrl;
    private String originalSketchUrl;
    private String moodTag;
    private Map<String, Object> choices;
    private OffsetDateTime createdAt;
}
