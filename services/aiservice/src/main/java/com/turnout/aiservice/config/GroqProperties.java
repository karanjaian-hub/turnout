package com.turnout.aiservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "turnout.groq")
public class GroqProperties {

    private String apiKey;
    private String apiUrl;
    private String model;
    private int maxTokens;
    private double temperature;

    public String getApiKey()            { return apiKey; }
    public String getApiUrl()            { return apiUrl; }
    public String getModel()             { return model; }
    public int getMaxTokens()            { return maxTokens; }
    public double getTemperature()       { return temperature; }

    public void setApiKey(String v)      { this.apiKey = v; }
    public void setApiUrl(String v)      { this.apiUrl = v; }
    public void setModel(String v)       { this.model = v; }
    public void setMaxTokens(int v)      { this.maxTokens = v; }
    public void setTemperature(double v) { this.temperature = v; }
}
