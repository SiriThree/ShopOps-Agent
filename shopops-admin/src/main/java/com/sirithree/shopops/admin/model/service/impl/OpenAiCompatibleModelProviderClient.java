package com.sirithree.shopops.admin.model.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.model.config.OpenAiCompatibleModelProperties;
import com.sirithree.shopops.admin.model.domain.ModelCallStatus;
import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;
import com.sirithree.shopops.admin.model.service.ModelProviderClient;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "shopops.model-gateway.openai-compatible.enabled", havingValue = "true")
public class OpenAiCompatibleModelProviderClient implements ModelProviderClient {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final OpenAiCompatibleModelProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleModelProviderClient(OpenAiCompatibleModelProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout())
                .build();
    }

    @Override
    public String providerCode() {
        return defaultString(properties.getProviderCode(), "openai-compatible").toLowerCase(Locale.ROOT);
    }

    @Override
    public String defaultModelName() {
        return defaultString(properties.getDefaultModel(), "gpt-4o-mini");
    }

    @Override
    public ModelInvokeResult invoke(ModelInvokeParam param) {
        RuntimeException lastFailure = null;
        int attempts = Math.max(1, properties.getMaxAttempts());
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(requestOf(param), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return resultOf(response.body(), param);
                }
                RuntimeException failure = new IllegalStateException("模型接口返回非 2xx: " + response.statusCode());
                if (!retryableStatus(response.statusCode()) || attempt >= attempts) {
                    throw failure;
                }
                lastFailure = failure;
                sleepBeforeRetry();
            } catch (IOException ex) {
                lastFailure = new IllegalStateException("模型接口调用失败: " + ex.getMessage(), ex);
                if (attempt >= attempts) {
                    throw lastFailure;
                }
                sleepBeforeRetry();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("模型接口调用被中断", ex);
            }
        }
        throw lastFailure == null ? new IllegalStateException("模型接口调用失败") : lastFailure;
    }

    private boolean retryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private void sleepBeforeRetry() {
        Duration backoff = properties.getRetryBackoff();
        if (backoff == null || backoff.isZero() || backoff.isNegative()) {
            return;
        }
        try {
            Thread.sleep(backoff.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("模型接口重试等待被中断", ex);
        }
    }

    private HttpRequest requestOf(ModelInvokeParam param) {
        Duration timeout = timeoutOf(param);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(chatCompletionsUri())
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(requestBodyOf(param))));
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder.header("Authorization", "Bearer " + properties.getApiKey().trim());
        }
        return builder.build();
    }

    private URI chatCompletionsUri() {
        String baseUrl = defaultString(properties.getBaseUrl(), "http://localhost:11434/v1");
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(normalized + "/chat/completions");
    }

    private Map<String, Object> requestBodyOf(ModelInvokeParam param) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", defaultString(param.getModelName(), defaultModelName()));
        body.put("messages", messagesOf(param));
        Object temperature = metadataValue(param, "temperature");
        if (temperature != null) {
            body.put("temperature", temperature);
        }
        Object maxTokens = metadataValue(param, "maxTokens");
        if (maxTokens != null) {
            body.put("max_tokens", maxTokens);
        }
        return body;
    }

    private List<Map<String, String>> messagesOf(ModelInvokeParam param) {
        List<Map<String, String>> messages = new ArrayList<>();
        Object systemPrompt = metadataValue(param, "systemPrompt");
        if (systemPrompt != null && !systemPrompt.toString().isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt.toString()));
        }
        messages.add(Map.of("role", "user", "content", param.getPrompt()));
        return messages;
    }

    private ModelInvokeResult resultOf(String responseBody, ModelInvokeParam param) {
        Map<String, Object> response = toMap(responseBody);
        ModelInvokeResult result = new ModelInvokeResult();
        result.setProviderCode(providerCode());
        result.setModelName(defaultString(stringValue(response.get("model")), defaultString(param.getModelName(), defaultModelName())));
        result.setOutputText(outputTextOf(response));
        result.setStatus(ModelCallStatus.SUCCESS);
        fillUsage(result, response.get("usage"));
        return result;
    }

    @SuppressWarnings("unchecked")
    private String outputTextOf(Map<String, Object> response) {
        Object choicesValue = response.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> choice)) {
            return "";
        }
        Object messageValue = choice.get("message");
        if (messageValue instanceof Map<?, ?> message) {
            return stringValue(message.get("content"));
        }
        return stringValue(choice.get("text"));
    }

    private void fillUsage(ModelInvokeResult result, Object usageValue) {
        if (!(usageValue instanceof Map<?, ?> usage)) {
            return;
        }
        result.setPromptTokens(intValue(usage.get("prompt_tokens")));
        result.setCompletionTokens(intValue(usage.get("completion_tokens")));
        result.setTotalTokens(intValue(usage.get("total_tokens")));
    }

    private Duration timeoutOf(ModelInvokeParam param) {
        if (param.getTimeoutMs() != null && param.getTimeoutMs() > 0) {
            return Duration.ofMillis(param.getTimeoutMs());
        }
        return properties.getTimeout();
    }

    private Object metadataValue(ModelInvokeParam param, String key) {
        if (param.getMetadata() == null) {
            return null;
        }
        return param.getMetadata().get(key);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("模型请求 JSON 序列化失败", ex);
        }
    }

    private Map<String, Object> toMap(String value) {
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("模型响应 JSON 反序列化失败", ex);
        }
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Integer.parseInt(value.toString());
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
