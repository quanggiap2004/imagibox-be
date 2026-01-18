package com.imagibox.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imagibox.domain.entity.*;
import com.imagibox.domain.enums.StoryMode;
import com.imagibox.domain.enums.StoryStatus;
import com.imagibox.dto.request.GenerateStoryRequest;
import com.imagibox.dto.request.NextChapterRequest;
import com.imagibox.dto.response.ChapterResponseDto;
import com.imagibox.dto.response.StoryResponseDto;
import com.imagibox.exception.ResourceNotFoundException;
import com.imagibox.exception.UnauthorizedException;
import com.imagibox.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final UserRepository userRepository;
    private final MoodTagRepository moodTagRepository;
    private final AiService aiService;
    private final ImageService imageService;
    private final ContentSafetyService contentSafetyService;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Transactional
    public StoryResponseDto generateOneShot(
            GenerateStoryRequest request,
            MultipartFile sketch,
            Long userId) {
        log.info("Generating one-shot story for user {}", userId);

        contentSafetyService.validatePrompt(request.getPrompt());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        rateLimitService.checkAndIncrementQuota(userId, user.getDailyQuota());

        Map<String, String> storyData = aiService.generateStory(request.getPrompt(), request.getMood());
        String rawResponse = storyData.get("raw");

        Map<String, String> parsedStory;
        try {
            parsedStory = objectMapper.readValue(rawResponse, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            throw new RuntimeException("Failed to generate story");
        }

        String title = parsedStory.getOrDefault("title", "Câu chuyện của bé");
        String content = parsedStory.getOrDefault("content", rawResponse);
        String moral = parsedStory.getOrDefault("moral", "");

        // Handle image generation - TODO: might want to make this fully async later
        String imageUrl = null;
        String sketchUrl = null;

        try {
            if (sketch != null && !sketch.isEmpty()) {
                sketchUrl = imageService.uploadToCloudinary(sketch);
                imageUrl = imageService.generateIllustration(
                        sketchUrl,
                        request.getPrompt(),
                        request.getMood()).join();
            } else {
                imageUrl = imageService.generateIllustrationFromText(
                        request.getPrompt(),
                        request.getMood()).join();
            }
        } catch (Exception e) {
            log.error("Image generation failed, continuing without image", e);
            imageUrl = null;
        }

        Story story = Story.builder()
                .user(user)
                .title(title)
                .status(StoryStatus.PUBLISHED)
                .mode(StoryMode.ONE_SHOT)
                .metadata(new HashMap<>(Map.of(
                        "moral", moral,
                        "mood", request.getMood() != null ? request.getMood() : "Vui vẻ")))
                .build();

        storyRepository.save(story);

        Map<String, Object> chapterContent = new HashMap<>();
        chapterContent.put("text", content);
        chapterContent.put("moral", moral);

        Chapter chapter = Chapter.builder()
                .story(story)
                .chapterNumber(1)
                .content(chapterContent)
                .userPrompt(request.getPrompt())
                .moodTag(request.getMood())
                .imageUrl(imageUrl)
                .originalSketchUrl(sketchUrl)
                .build();

        chapterRepository.save(chapter);

        // Track mood for parent dashboard
        if (request.getMood() != null) {
            MoodTag moodTag = MoodTag.builder()
                    .chapter(chapter)
                    .moodTag(request.getMood())
                    .build();
            moodTagRepository.save(moodTag);
        }

        log.info("Story created successfully: {}", story.getId());

        return mapToStoryResponse(story, Collections.singletonList(chapter));
    }

    @Transactional
    public StoryResponseDto generateInteractive(
            GenerateStoryRequest request,
            MultipartFile sketch,
            Long userId) {
        log.info("Generating interactive story for user {}", userId);

        contentSafetyService.validatePrompt(request.getPrompt());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        rateLimitService.checkAndIncrementQuota(userId, user.getDailyQuota());

        // Generate first chapter content
        Map<String, String> storyData = aiService.generateStory(request.getPrompt(), request.getMood());
        String rawResponse = storyData.get("raw");

        Map<String, String> parsedStory;
        try {
            parsedStory = objectMapper.readValue(rawResponse, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            throw new RuntimeException("Failed to generate story");
        }

        String title = parsedStory.getOrDefault("title", "Câu chuyện tương tác");
        String content = parsedStory.getOrDefault("content", rawResponse);

        // Handle image generation
        String imageUrl = null;
        String sketchUrl = null;

        try {
            if (sketch != null && !sketch.isEmpty()) {
                sketchUrl = imageService.uploadToCloudinary(sketch);
                imageUrl = imageService.generateIllustration(
                        sketchUrl,
                        request.getPrompt(),
                        request.getMood()).join();
            } else {
                imageUrl = imageService.generateIllustrationFromText(
                        request.getPrompt(),
                        request.getMood()).join();
            }
        } catch (Exception e) {
            log.error("Image generation failed, continuing without image", e);
            imageUrl = null;
        }

        // Create interactive story
        Story story = Story.builder()
                .user(user)
                .title(title)
                .status(StoryStatus.PUBLISHED)
                .mode(StoryMode.INTERACTIVE)
                .metadata(new HashMap<>(Map.of(
                        "mood", request.getMood() != null ? request.getMood() : "Vui vẻ")))
                .build();

        storyRepository.save(story);

        // Create first chapter
        Map<String, Object> chapterContent = new HashMap<>();
        chapterContent.put("text", content);

        Chapter chapter = Chapter.builder()
                .story(story)
                .chapterNumber(1)
                .content(chapterContent)
                .userPrompt(request.getPrompt())
                .moodTag(request.getMood())
                .imageUrl(imageUrl)
                .originalSketchUrl(sketchUrl)
                .build();

        chapterRepository.save(chapter);

        // Track mood for parent dashboard
        if (request.getMood() != null) {
            MoodTag moodTag = MoodTag.builder()
                    .chapter(chapter)
                    .moodTag(request.getMood())
                    .build();
            moodTagRepository.save(moodTag);
        }

        log.info("Interactive story created successfully: {}", story.getId());

        return mapToStoryResponse(story, Collections.singletonList(chapter));
    }

    @Transactional
    public ChapterResponseDto generateNextChapter(Long storyId, NextChapterRequest request, Long userId) {
        log.info("Generating next chapter for story {}", storyId);

        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));

        if (!story.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only continue your own stories");
        }

        if (story.getMode() != StoryMode.INTERACTIVE) {
            throw new IllegalArgumentException("Only interactive stories can have multiple chapters");
        }

        List<Chapter> previousChapters = chapterRepository.findByStoryIdOrderByChapterNumberAsc(storyId);
        String context = buildContext(previousChapters);

        Map<String, String> chapterData = aiService.generateNextChapter(context, request.getUserChoice());
        String rawResponse = chapterData.get("raw");

        Map<String, String> parsedChapter;
        try {
            parsedChapter = objectMapper.readValue(rawResponse, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            throw new RuntimeException("Failed to generate chapter");
        }

        String content = parsedChapter.getOrDefault("content", rawResponse);
        String choiceA = parsedChapter.get("choiceA");
        String choiceB = parsedChapter.get("choiceB");

        // FIXME: sometimes image generation times out here
        String imageUrl = null;
        try {
            imageUrl = imageService.generateIllustrationFromText(content, story.getMetadata().get("mood").toString())
                    .join();
        } catch (Exception e) {
            log.error("Image generation failed", e);
        }

        int nextChapterNumber = previousChapters.size() + 1;

        Map<String, Object> chapterContent = new HashMap<>();
        chapterContent.put("text", content);

        Map<String, Object> choices = new HashMap<>();
        if (choiceA != null)
            choices.put("A", choiceA);
        if (choiceB != null)
            choices.put("B", choiceB);

        Chapter chapter = Chapter.builder()
                .story(story)
                .chapterNumber(nextChapterNumber)
                .content(chapterContent)
                .userPrompt(request.getUserChoice() != null ? request.getUserChoice() : "Continue")
                .imageUrl(imageUrl)
                .choices(choices.isEmpty() ? null : choices)
                .build();

        chapterRepository.save(chapter);

        log.info("Chapter {} created for story {}", nextChapterNumber, storyId);

        return mapToChapterResponse(chapter);
    }

    public Page<StoryResponseDto> getStoriesByUser(Long userId, Pageable pageable) {
        return storyRepository.findByUserId(userId, pageable)
                .map(story -> {
                    List<Chapter> chapters = chapterRepository.findByStoryIdOrderByChapterNumberAsc(story.getId());
                    return mapToStoryResponse(story, chapters);
                });
    }

    public StoryResponseDto getStoryById(Long storyId, Long userId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));

        if (!story.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only view your own stories");
        }

        List<Chapter> chapters = chapterRepository.findByStoryIdOrderByChapterNumberAsc(storyId);
        return mapToStoryResponse(story, chapters);
    }

    @Transactional
    public void deleteStory(Long storyId, Long userId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story not found"));

        if (!story.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only delete your own stories");
        }

        storyRepository.delete(story);
        log.info("Story {} deleted by user {}", storyId, userId);
    }

    private String buildContext(List<Chapter> chapters) {
        return chapters.stream()
                .map(ch -> "Chương " + ch.getChapterNumber() + ": " +
                        ch.getContent().get("text"))
                .collect(Collectors.joining("\n\n"));
    }

    private StoryResponseDto mapToStoryResponse(Story story, List<Chapter> chapters) {
        return StoryResponseDto.builder()
                .id(story.getId())
                .title(story.getTitle())
                .status(story.getStatus().name())
                .mode(story.getMode().name())
                .metadata(story.getMetadata())
                .createdAt(story.getCreatedAt())
                .chapters(chapters.stream()
                        .map(this::mapToChapterResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    private ChapterResponseDto mapToChapterResponse(Chapter chapter) {
        return ChapterResponseDto.builder()
                .id(chapter.getId())
                .chapterNumber(chapter.getChapterNumber())
                .content(chapter.getContent())
                .imageUrl(chapter.getImageUrl())
                .originalSketchUrl(chapter.getOriginalSketchUrl())
                .moodTag(chapter.getMoodTag())
                .choices(chapter.getChoices())
                .createdAt(chapter.getCreatedAt())
                .build();
    }
}
