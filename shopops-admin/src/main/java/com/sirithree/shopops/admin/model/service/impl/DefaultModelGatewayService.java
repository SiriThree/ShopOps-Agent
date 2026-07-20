package com.sirithree.shopops.admin.model.service.impl;

import com.sirithree.shopops.admin.model.domain.ModelCallLogDto;
import com.sirithree.shopops.admin.model.domain.ModelCallLogQueryParam;
import com.sirithree.shopops.admin.model.domain.ModelCallStatus;
import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;
import com.sirithree.shopops.admin.model.service.ModelGatewayService;
import com.sirithree.shopops.admin.model.service.ModelProviderClient;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class DefaultModelGatewayService implements ModelGatewayService {
    private final AtomicLong callIdGenerator = new AtomicLong(1);
    private final Map<Long, ModelCallLogDto> logs = new ConcurrentHashMap<>();
    private final Map<String, ModelProviderClient> providers;

    public DefaultModelGatewayService(List<ModelProviderClient> providerClients) {
        this.providers = providerClients.stream()
                .collect(java.util.stream.Collectors.toMap(
                        client -> client.providerCode().toLowerCase(Locale.ROOT),
                        client -> client
                ));
    }

    @Override
    public ModelInvokeResult invoke(Long tenantId, Long shopId, Long userId, String username, ModelInvokeParam param) {
        long started = System.nanoTime();
        Long callId = callIdGenerator.getAndIncrement();
        String providerCode = defaultString(param.getProviderCode(), "echo").toLowerCase(Locale.ROOT);
        ModelProviderClient provider = providers.get(providerCode);
        if (provider == null) {
            ModelInvokeResult result = failedResult(callId, providerCode, param.getModelName(), "PROVIDER_NOT_FOUND",
                    "模型供应商不存在: " + providerCode, started);
            logs.put(callId, logOf(callId, tenantId, shopId, userId, username, param, result));
            return result;
        }

        ModelInvokeResult result;
        try {
            result = provider.invoke(param);
            result.setStatus(ModelCallStatus.SUCCESS);
        } catch (RuntimeException ex) {
            result = failedResult(callId, providerCode, param.getModelName(), "MODEL_INVOKE_FAILED", ex.getMessage(), started);
        }
        result.setCallId(callId);
        result.setProviderCode(provider.providerCode());
        result.setModelName(defaultString(param.getModelName(), provider.defaultModelName()));
        fillUsage(result, param.getPrompt(), result.getOutputText());
        result.setLatencyMs(elapsedMs(started));
        logs.put(callId, logOf(callId, tenantId, shopId, userId, username, param, result));
        return result;
    }

    @Override
    public CommonPage<ModelCallLogDto> listLogs(Long tenantId, Long shopId, ModelCallLogQueryParam queryParam) {
        ModelCallLogQueryParam query = queryParam == null ? new ModelCallLogQueryParam() : queryParam;
        List<ModelCallLogDto> filtered = logs.values().stream()
                .filter(log -> tenantId.equals(log.getTenantId()) && shopId.equals(log.getShopId()))
                .filter(log -> blank(query.getProviderCode()) || query.getProviderCode().equalsIgnoreCase(log.getProviderCode()))
                .filter(log -> blank(query.getModelName()) || query.getModelName().equalsIgnoreCase(log.getModelName()))
                .filter(log -> blank(query.getStatus()) || query.getStatus().equalsIgnoreCase(log.getStatus()))
                .filter(log -> blank(query.getTraceId()) || query.getTraceId().equals(log.getTraceId()))
                .filter(log -> query.getTaskId() == null || query.getTaskId().equals(log.getTaskId()))
                .sorted(Comparator.comparing(ModelCallLogDto::getCallId).reversed())
                .toList();
        List<ModelCallLogDto> page = filtered.stream()
                .skip(query.offset())
                .limit(query.safePageSize())
                .toList();
        return CommonPage.of(page, query.safePageNum(), query.safePageSize(), (long) filtered.size());
    }

    private ModelInvokeResult failedResult(Long callId, String providerCode, String modelName, String errorCode,
                                           String errorMessage, long started) {
        ModelInvokeResult result = new ModelInvokeResult();
        result.setCallId(callId);
        result.setProviderCode(providerCode);
        result.setModelName(defaultString(modelName, "unknown"));
        result.setStatus(ModelCallStatus.FAILURE);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        result.setLatencyMs(elapsedMs(started));
        fillUsage(result, "", "");
        return result;
    }

    private ModelCallLogDto logOf(Long callId, Long tenantId, Long shopId, Long userId, String username,
                                  ModelInvokeParam param, ModelInvokeResult result) {
        ModelCallLogDto log = new ModelCallLogDto();
        log.setCallId(callId);
        log.setTenantId(tenantId);
        log.setShopId(shopId);
        log.setUserId(userId);
        log.setUsername(username);
        log.setProviderCode(result.getProviderCode());
        log.setModelName(result.getModelName());
        log.setPromptCode(param.getPromptCode());
        log.setPromptVersion(param.getPromptVersion());
        log.setTraceId(param.getTraceId());
        log.setTaskId(param.getTaskId());
        log.setReportId(param.getReportId());
        log.setStatus(result.getStatus());
        log.setPromptTokens(result.getPromptTokens());
        log.setCompletionTokens(result.getCompletionTokens());
        log.setTotalTokens(result.getTotalTokens());
        log.setLatencyMs(result.getLatencyMs());
        log.setErrorCode(result.getErrorCode());
        log.setErrorMessage(result.getErrorMessage());
        log.setPromptPreview(preview(param.getPrompt()));
        log.setOutputPreview(preview(result.getOutputText()));
        log.setMetadata(param.getMetadata() == null ? Map.of() : param.getMetadata());
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }

    private void fillUsage(ModelInvokeResult result, String prompt, String output) {
        int promptTokens = estimateTokens(prompt);
        int completionTokens = estimateTokens(output);
        result.setPromptTokens(promptTokens);
        result.setCompletionTokens(completionTokens);
        result.setTotalTokens(promptTokens + completionTokens);
    }

    private int estimateTokens(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(value.length() / 4.0));
    }

    private String preview(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 240 ? value : value.substring(0, 240);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private long elapsedMs(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000);
    }
}
