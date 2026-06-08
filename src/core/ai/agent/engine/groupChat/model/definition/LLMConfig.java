package ai.agent.engine.groupChat.model.definition;

import java.io.Serializable;

public class LLMConfig implements Serializable {
    private String configCode;
    private String configName;
    private String baseUrl;
    private String apiKey;
    private String model;
    private Long    maxTokens;
    private Double temperature;
    private Double topP;
    private Double presencePenalty;
    private Double frequencyPenalty;

    public LLMConfig() {
    }

    public LLMConfig(String configCode, String configName, String baseUrl, String apiKey, String model) {
        this.configCode = configCode;
        this.configName = configName;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    public LLMConfig(String configCode, String configName, String baseUrl, String apiKey, String model, Long maxTokens, Double temperature, Double topP, Double presencePenalty, Double frequencyPenalty) {
        this.configCode = configCode;
        this.configName = configName;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.topP = topP;
        this.presencePenalty = presencePenalty;
        this.frequencyPenalty = frequencyPenalty;
    }

    public String getConfigCode() {
        return configCode;
    }


    public LLMConfig setConfigCode(String configCode) {
        this.configCode = configCode;
        return this;
    }

    public String getConfigName() {
        return configName;
    }

    public LLMConfig setConfigName(String configName) {
        this.configName = configName;
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public LLMConfig setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public String getApiKey() {
        return apiKey;
    }

    public LLMConfig setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getModel() {
        return model;
    }

    public LLMConfig setModel(String model) {
        this.model = model;
        return this;
    }

    public Long getMaxTokens() {
        return maxTokens;
    }

    public LLMConfig setMaxTokens(Long maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    public Double getTemperature() {
        return temperature;
    }

    public LLMConfig setTemperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    public Double getTopP() {
        return topP;
    }

    public LLMConfig setTopP(Double topP) {
        this.topP = topP;
        return this;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public LLMConfig setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
        return this;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public LLMConfig setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
        return this;
    }
}

