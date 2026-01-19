package com.imagibox.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * Configure RestClient with longer timeout for AI requests
     * Default timeout is too short for complex story generation
     */
    @Bean
    public RestClientCustomizer restClientCustomizer() {
        return restClientBuilder -> {
            // Create HttpClient with longer timeout
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(60))
                    .build();

            // Create request factory with the configured HttpClient
            JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(Duration.ofSeconds(60));

            restClientBuilder.requestFactory(requestFactory);
        };
    }
}
