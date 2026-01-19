package com.imagibox.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.imagibox.config.CloudinaryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;
    private final AiService aiService;

    @Value("${GEMINI_API_KEY}")
    private String geminiApiKey;

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
    public CompletableFuture<Map<String, String>> generateIllustration(MultipartFile sketch, String prompt,
            String mood) {
        log.info("Generating illustration from sketch: {}", sketch.getOriginalFilename());

        try {
            // 1. Extract bytes from sketch
            byte[] imageBytes = sketch.getBytes();

            // 2. Upload original to Cloudinary for storage/reference
            String sketchUrl = uploadToCloudinary(sketch);

            // 3. Generate enhanced image prompt
            String imagePrompt = aiService.generateImagePrompt(prompt, mood);
            log.info("Image prompt length: {}", imagePrompt.length());
            log.info("Image prompt: {}", imagePrompt);

            // 4. Call Gemini API for image-to-image generation
            byte[] generatedImageBytes = callGeminiImageToImage(imageBytes, imagePrompt);

            // 5. Upload generated image to Cloudinary
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    generatedImageBytes,
                    ObjectUtils.asMap("folder", cloudinaryProperties.getFolder()));
            String generatedImageUrl = (String) uploadResult.get("secure_url");

            log.info("Illustration generated successfully. Sketch: {}, Generated: {}", sketchUrl, generatedImageUrl);

            // 6. Return both URLs
            return CompletableFuture.completedFuture(Map.of(
                    "sketchUrl", sketchUrl,
                    "generatedUrl", generatedImageUrl));
        } catch (Exception e) {
            log.error("Failed to generate illustration", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("taskExecutor")
    public CompletableFuture<String> generateIllustrationFromText(String prompt, String mood) {
        log.info("Generating illustration from text prompt: {}", prompt);

        try {
            String imagePrompt = aiService.generateImagePrompt(prompt, mood);

            // Call Gemini API for text-to-image generation
            byte[] generatedImageBytes = callGeminiTextToImage(imagePrompt);

            // Upload to Cloudinary
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    generatedImageBytes,
                    ObjectUtils.asMap("folder", cloudinaryProperties.getFolder()));
            String generatedImageUrl = (String) uploadResult.get("secure_url");

            log.info("Illustration generated successfully: {}", generatedImageUrl);
            return CompletableFuture.completedFuture(generatedImageUrl);
        } catch (Exception e) {
            log.error("Failed to generate illustration", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    private byte[] callGeminiImageToImage(byte[] imageBytes, String prompt) throws IOException {
        log.info("Calling Gemini image-to-image API (image size: {} bytes)", imageBytes.length);

        try (Client client = Client.builder().apiKey(geminiApiKey).build()) {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("IMAGE")
                    .build();

            Content content = Content.fromParts(
                    Part.fromText(prompt),
                    Part.fromBytes(imageBytes, "image/jpeg"));

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash-image",
                    content,
                    config);

            for (Part part : response.parts()) {
                if (part.inlineData().isPresent()) {
                    var blob = part.inlineData().get();
                    if (blob.data().isPresent()) {
                        byte[] generatedBytes = blob.data().get();
                        log.info("Gemini generated image successfully (size: {} bytes)", generatedBytes.length);
                        return generatedBytes;
                    }
                }
            }

            throw new RuntimeException("No image generated in Gemini response");
        }
    }

    private byte[] callGeminiTextToImage(String prompt) throws IOException {
        log.info("Calling Gemini text-to-image API");

        try (Client client = Client.builder().apiKey(geminiApiKey).build()) {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("IMAGE")
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash-image",
                    prompt,
                    config);

            for (Part part : response.parts()) {
                if (part.inlineData().isPresent()) {
                    var blob = part.inlineData().get();
                    if (blob.data().isPresent()) {
                        byte[] generatedBytes = blob.data().get();
                        log.info("Gemini generated image successfully (size: {} bytes)", generatedBytes.length);
                        return generatedBytes;
                    }
                }
            }

            throw new RuntimeException("No image generated in Gemini response");
        }
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
