package com.imagibox.controller;

import com.imagibox.dto.request.GenerateStoryRequest;
import com.imagibox.dto.request.NextChapterRequest;
import com.imagibox.dto.response.ChapterResponseDto;
import com.imagibox.dto.response.StoryResponseDto;
import com.imagibox.service.JwtService;
import com.imagibox.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/stories")
@Tag(name = "Stories", description = "Story creation and management endpoints")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;
    private final JwtService jwtService;

    @PostMapping(value = "/generate-one-shot", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Generate a one-shot story", description = "Creates a complete story with optional sketch. Prompt is mandatory, sketch is optional.")
    public ResponseEntity<StoryResponseDto> generateOneShot(
            @RequestPart("request") @Valid GenerateStoryRequest request,
            @RequestPart(value = "sketch", required = false) MultipartFile sketch,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtService.extractUserId(token);

        StoryResponseDto story = storyService.generateOneShot(request, sketch, userId);
        return ResponseEntity.ok(story);
    }

    @PostMapping("/{storyId}/chapters/next")
    @Operation(summary = "Generate next chapter for interactive story")
    public ResponseEntity<ChapterResponseDto> generateNextChapter(
            @PathVariable Long storyId,
            @RequestBody NextChapterRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtService.extractUserId(token);

        ChapterResponseDto chapter = storyService.generateNextChapter(storyId, request, userId);
        return ResponseEntity.ok(chapter);
    }

    @GetMapping
    @Operation(summary = "Get all stories for the logged-in user with pagination")
    public ResponseEntity<Page<StoryResponseDto>> getMyStories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtService.extractUserId(token);

        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(storyService.getStoriesByUser(userId, pageable));
    }

    @GetMapping("/{storyId}")
    @Operation(summary = "Get story details by ID")
    public ResponseEntity<StoryResponseDto> getStoryById(
            @PathVariable Long storyId,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtService.extractUserId(token);

        return ResponseEntity.ok(storyService.getStoryById(storyId, userId));
    }

    @DeleteMapping("/{storyId}")
    @Operation(summary = "Delete a story")
    public ResponseEntity<Void> deleteStory(
            @PathVariable Long storyId,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        Long userId = jwtService.extractUserId(token);

        storyService.deleteStory(storyId, userId);
        return ResponseEntity.noContent().build();
    }
}
