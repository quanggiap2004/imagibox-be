package com.imagibox.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateStoryRequest {

    @NotBlank(message = "Story prompt is required")
    @Size(min = 10, max = 500, message = "Prompt must be between 10 and 500 characters")
    private String prompt;

    private String mood;

    private String mode; // ONE_SHOT or INTERACTIVE
}
