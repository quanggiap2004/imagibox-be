package com.imagibox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final ChatClient chatClient;

    private static final String STORY_GENERATION_TEMPLATE = """
            Bạn là một nhà văn chuyên viết truyện cho trẻ em từ 5-12 tuổi.

            **Nhiệm vụ:** Viết một câu chuyện ngắn khoảng 300-400 từ dựa trên ý tưởng của trẻ.

            **Ý tưởng của trẻ:** {userPrompt}

            **Tâm trạng/Cảm xúc:** {mood}

            **Yêu cầu:**
            1. Nội dung phải phù hợp với trẻ em, tích cực, lạc quan
            2. Ngôn ngữ đơn giản, dễ hiểu
            3. Có bài học ý nghĩa (tình bạn, lòng dũng cảm, sự tốt bụng, v.v.)
            4. Kết thúc có hậu
            5. Tránh nội dung bạo lực, đáng sợ hoặc không phù hợp

            Hãy viết câu chuyện theo định dạng JSON:
            {{
              "title": "Tiêu đề câu chuyện",
              "content": "Nội dung câu chuyện đầy đủ",
              "moral": "Bài học rút ra"
            }}
            """;

    private static final String CHAPTER_CONTINUATION_TEMPLATE = """
            Bạn là một nhà văn chuyên viết truyện cho trẻ em.

            **Bối cảnh truyện trước đó:**
            {context}

            **Lựa chọn của trẻ:** {userChoice}

            **Nhiệm vụ:** Viết tiếp chương kế tiếp (khoảng 200-300 từ) dựa trên lựa chọn của trẻ.

            **Yêu cầu:**
            1. Nội dung phải phù hợp với trẻ em
            2. Tiếp nối mạch truyện một cách tự nhiên
            3. Cuối chương đưa ra 2 lựa chọn mới (A và B) để trẻ quyết định

            Hãy trả lời theo định dạng JSON:
            {{
              "content": "Nội dung chương mới",
              "choiceA": "Lựa chọn A - Mô tả ngắn gọn",
              "choiceB": "Lựa chọn B - Mô tả ngắn gọn"
            }}
            """;

    private static final String IMAGE_PROMPT_TEMPLATE = """
            Create a detailed image generation prompt for a children's book illustration based on this story concept:

            Story idea: {userPrompt}
            Mood: {mood}

            Generate a prompt for Stable Diffusion that describes a colorful, child-friendly, cartoon-style illustration.
            Include: art style (3D cartoon, vibrant colors), main subjects, setting, mood, and artistic details.

            Return ONLY the image prompt text, no additional explanation.
            """;

    public Map<String, String> generateStory(String userPrompt, String mood) {
        log.info("Generating story with prompt: {} and mood: {}", userPrompt, mood);

        PromptTemplate promptTemplate = new PromptTemplate(STORY_GENERATION_TEMPLATE);
        Prompt prompt = promptTemplate.create(Map.of(
                "userPrompt", userPrompt,
                "mood", mood != null ? mood : "Vui vẻ"));

        String response = chatClient.prompt(prompt).call().content();
        log.debug("AI response: {}", response);

        return parseJsonResponse(response);
    }

    public Map<String, String> generateNextChapter(String context, String userChoice) {
        log.info("Generating next chapter with choice: {}", userChoice);

        PromptTemplate promptTemplate = new PromptTemplate(CHAPTER_CONTINUATION_TEMPLATE);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", context,
                "userChoice", userChoice != null ? userChoice : "Tiếp tục phiêu lưu"));

        String response = chatClient.prompt(prompt).call().content();
        return parseJsonResponse(response);
    }

    public String generateImagePrompt(String userPrompt, String mood) {
        log.info("Generating image prompt for: {}", userPrompt);

        PromptTemplate promptTemplate = new PromptTemplate(IMAGE_PROMPT_TEMPLATE);
        Prompt prompt = promptTemplate.create(Map.of(
                "userPrompt", userPrompt,
                "mood", mood != null ? mood : "Happy"));

        return chatClient.prompt(prompt).call().content().trim();
    }

    private Map<String, String> parseJsonResponse(String response) {
        // TODO: this is kinda hacky, should probably use proper JSON parsing here
        try {
            String cleaned = response.trim()
                    .replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*", "")
                    .trim();

            return Map.of("raw", cleaned);
        } catch (Exception e) {
            log.error("Failed to parse AI response", e);
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage());
        }
    }
}
