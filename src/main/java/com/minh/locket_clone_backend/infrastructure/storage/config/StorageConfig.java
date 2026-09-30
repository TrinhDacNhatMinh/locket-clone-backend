package com.minh.locket_clone_backend.infrastructure.storage.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public StorageConfig(@Value("${app.cloudinary.cloud-name}") String cloudName,
                         @Value("${app.cloudinary.api-key}") String apiKey,
                         @Value("${app.cloudinary.api-secret}") String apiSecret) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret));
    }
}
