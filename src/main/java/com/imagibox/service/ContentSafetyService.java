package com.imagibox.service;

import com.imagibox.exception.ContentUnsafeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ContentSafetyService {

    // Basic inappropriate keywords - expand as needed
    private static final List<String> INAPPROPRIATE_KEYWORDS = Arrays.asList(
            "kill", "death", "violence", "weapon", "gun", "blood",
            "chết", "giết", "bạo lực", "vũ khí", "súng", "máu");

    public void validatePrompt(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }

        for (String keyword : INAPPROPRIATE_KEYWORDS) {
            String regex = "(?i)(?U)\\b" + Pattern.quote(keyword) + "\\b";
            if (Pattern.compile(regex).matcher(prompt).find()) {
                log.warn("Inappropriate content detected: {}", keyword);
                throw new ContentUnsafeException("Nội dung chứa từ ngữ không phù hợp: " + keyword);
            }
        }

        log.debug("Content safety check passed");
    }

    public boolean isContentSafe(String content) {
        try {
            validatePrompt(content);
            return true;
        } catch (ContentUnsafeException e) {
            return false;
        }
    }
}
