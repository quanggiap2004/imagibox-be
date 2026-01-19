package com.imagibox.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    private final ChatClient chatClient;

    @GetMapping("/spring-ai")
    public ResponseEntity<?> testSpringAi() {
        Instant start = Instant.now();

        try {
            String testPrompt = """
                    Bạn là một nhà văn chuyên viết truyện cho trẻ em từ 5-12 tuổi.

                    **Nhiệm vụ:** Viết một câu chuyện ngắn khoảng 300-400 từ dựa trên ý tưởng của trẻ.

                    **Ý tưởng của trẻ:** A brave astronaut exploring a magical planet with friendly aliens

                    **Tâm trạng/Cảm xúc:** Adventurous and Exciting

                    **Yêu cầu:**
                    1. Nội dung phải phù hợp với trẻ em, tích cực, lạc quan
                    2. Ngôn ngữ đơn giản, dễ hiểu
                    3. Có bài học ý nghĩa (tình bạn, lòng dũng cảm, sự tốt bụng, khám phá, v.v.)
                    4. Kết thúc có hậu
                    5. Tránh nội dung bạo lực, đáng sợ hoặc không phù hợp
                    6. Khuyến khích sự tò mò và khám phá

                    Hãy viết câu chuyện theo định dạng JSON:
                    {
                      "title": "Tiêu đề câu chuyện",
                      "content": "Nội dung câu chuyện đầy đủ",
                      "moral": "Bài học rút ra"
                    }
                    """;

            String response = chatClient.prompt(testPrompt).call().content();

            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            log.info("SUCCESS! Spring AI response received in {} ms with {} characters",
                    duration.toMillis(), response.length());

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "responseTimeMs", duration.toMillis(),
                    "responseLength", response.length(),
                    "response", response,
                    "method", "Spring AI ChatClient"));

        } catch (Exception e) {
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            log.error("FAILED after {} ms: {}", duration.toMillis(), e.getMessage(), e);

            return ResponseEntity.ok(Map.of(
                    "status", "FAILED",
                    "failureTimeMs", duration.toMillis(),
                    "errorType", e.getClass().getSimpleName(),
                    "errorMessage", e.getMessage(),
                    "stackTrace", e.getCause() != null ? e.getCause().getMessage() : "No cause",
                    "method", "Spring AI ChatClient"));
        }
    }

    @GetMapping("/story-prompt")
    public ResponseEntity<?> testStoryPrompt() {
        log.info("Testing with REALISTIC story generation prompt...");

        Instant start = Instant.now();

        try {
            String storyPrompt = """
                    Bạn là một nhà văn chuyên viết truyện cho trẻ em từ 5-12 tuổi.

                    **Nhiệm vụ:** Viết một câu chuyện ngắn khoảng 300-400 từ dựa trên ý tưởng của trẻ.

                    **Ý tưởng của trẻ:** A princess fighting a scary monster

                    **Tâm trạng/Cảm xúc:** Scary

                    **Yêu cầu:**
                    1. Nội dung phải phù hợp với trẻ em, tích cực, lạc quan
                    2. Ngôn ngữ đơn giản, dễ hiểu
                    3. Có bài học ý nghĩa (tình bạn, lòng dũng cảm, sự tốt bụng, v.v.)
                    4. Kết thúc có hậu
                    5. Tránh nội dung bạo lực, đáng sợ hoặc không phù hợp

                    Hãy viết câu chuyện theo định dạng JSON:
                    {
                      "title": "Tiêu đề câu chuyện",
                      "content": "Nội dung câu chuyện đầy đủ",
                      "moral": "Bài học rút ra"
                    }
                    """;

            String response = chatClient.prompt(storyPrompt).call().content();

            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "responseTimeMs", duration.toMillis(),
                    "responseLength", response.length(),
                    "response", response.substring(0, Math.min(500, response.length())) + "...",
                    "method", "Spring AI - Realistic Story Prompt"));

        } catch (Exception e) {
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            log.error("FAILED after {} ms: {}", duration.toMillis(), e.getMessage(), e);

            return ResponseEntity.ok(Map.of(
                    "status", "FAILED",
                    "failureTimeMs", duration.toMillis(),
                    "errorType", e.getClass().getSimpleName(),
                    "errorMessage", e.getMessage(),
                    "rootCause", e.getCause() != null ? e.getCause().getClass().getSimpleName() : "No cause",
                    "rootCauseMessage", e.getCause() != null ? e.getCause().getMessage() : "No cause",
                    "method", "Spring AI - Realistic Story Prompt"));
        }
    }

    @GetMapping("/gemini")
    public ResponseEntity<?> testGeminiConnection() {
        log.info("Testing Gemini API connection...");

        Instant start = Instant.now();

        try {
            RestTemplate restTemplate = new RestTemplate();

            String url = "https://generativelanguage.googleapis.com/v1beta/openai/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = Map.of(
                    "model", "gemini-2.5-flash",
                    "messages", new Object[] {
                            Map.of(
                                    "role", "user",
                                    "content", "Say 'Hello' in one word")
                    },
                    "max_tokens", 10);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "responseTimeMs", duration.toMillis(),
                    "statusCode", response.getStatusCode().value(),
                    "response", response.getBody()));

        } catch (Exception e) {
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);

            log.error("FAILED after {} ms: {}", duration.toMillis(), e.getMessage(), e);

            return ResponseEntity.ok(Map.of(
                    "status", "FAILED",
                    "failureTimeMs", duration.toMillis(),
                    "errorType", e.getClass().getSimpleName(),
                    "errorMessage", e.getMessage(),
                    "apiKey", "***" + apiKey.substring(apiKey.length() - 4)));
        }
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(Map.of("status", "pong", "timestamp", System.currentTimeMillis()));
    }
}
