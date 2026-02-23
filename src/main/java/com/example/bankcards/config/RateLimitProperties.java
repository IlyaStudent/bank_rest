package com.example.bankcards.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private LimitRule login = new LimitRule();
    private LimitRule register = new LimitRule();
    private LimitRule transfers = new LimitRule();
    private LimitRule general = new LimitRule();

    @Data
    public static class LimitRule {
        private int capacity = 10;
        private int minutes = 1;
    }
}
