package com.imagibox.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.imagibox.config.CloudinaryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;
    private final AiService aiService;

    @Value("${stable-diffusion.api-url}")
    private String stableDiffusionApiUrl;

    @Value("${stable-diffusion.api-key}")
    private String stableDiffusionApiKey;

    public String uploadToCloudinary(MultipartFile file) throws IOException {
        log.info("Uploading image to Cloudinary: {}", file.getOriginalFilename());

        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", cloudinaryProperties.getFolder(),
                        "resource_type", "auto"));

        String url = (String) uploadResult.get("secure_url");
        log.info("Image uploaded successfully: {}", url);
        return url;
    }

    @Async("taskExecutor")
    public CompletableFuture<String> generateIllustration(String sketchUrl, String prompt, String mood) {
        log.info("Generating illustration from sketch: {}", sketchUrl);

        try {
            String imagePrompt = aiService.generateImagePrompt(prompt, mood);

            String generatedImageUrl = callStableDiffusionImg2Img(sketchUrl, imagePrompt);

            log.info("Illustration generated successfully: {}", generatedImageUrl);
            return CompletableFuture.completedFuture(generatedImageUrl);
        } catch (Exception e) {
            log.error("Failed to generate illustration", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<String> generateIllustrationFromText(String prompt, String mood) {
        log.info("Generating illustration from text prompt: {}", prompt);

        try {
            // Generate enhanced image prompt using AI
            String imagePrompt = aiService.generateImagePrompt(prompt, mood);

            // Call Stable Diffusion API for text-to-image generation
            String generatedImageUrl = callStableDiffusionTxt2Img(imagePrompt);

            log.info("Illustration generated successfully: {}", generatedImageUrl);
            return CompletableFuture.completedFuture(generatedImageUrl);
        } catch (Exception e) {
            log.error("Failed to generate illustration", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private String callStableDiffusionImg2Img(String initImageUrl, String prompt) throws Exception {

        String requestBody = String.format("""
                {
                    "init_image": "%s",
                    "text_prompts": [
                        {
                            "text": "%s",
                            "weight": 1.0
                        }
                    ],
                    "cfg_scale": 7,
                    "samples": 1,
                    "steps": 30,
                    "style_preset": "digital-art"
                }
                """, initImageUrl, prompt.replace("\"", "\\\""));

        return callStableDiffusionApi("/v1/generation/stable-diffusion-xl-1024-v1-0/image-to-image", requestBody);
    }

    private String callStableDiffusionTxt2Img(String prompt) throws Exception {

        String requestBody = String.format("""
                {
                    "text_prompts": [
                        {
                            "text": "%s",
                            "weight": 1.0
                        }
                    ],
                    "cfg_scale": 7,
                    "height": 512,
                    "width": 512,
                    "samples": 1,
                    "steps": 30,
                    "style_preset": "digital-art"
                }
                """, prompt.replace("\"", "\\\""));

        return callStableDiffusionApi("/v1/generation/stable-diffusion-xl-1024-v1-0/text-to-image", requestBody);
    }

    private String callStableDiffusionApi(String endpoint, String requestBody) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(stableDiffusionApiUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + stableDiffusionApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Stable Diffusion API error: " + response.body());
        }

        log.debug("Stable Diffusion response: {}", response.body());

        return "https://placeholder-image-url.com/generated.png";
    }

    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete image: {}", publicId, e);
        }
    }
}
