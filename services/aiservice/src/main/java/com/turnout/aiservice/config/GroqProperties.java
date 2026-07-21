package com.turnout.aiservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "turnout.groq")
public class GroqProperties {

    private String apiKey;
    private String apiUrl;
    private String model;
    private int maxTokens;
    private double temperature;

}
