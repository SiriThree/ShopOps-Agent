package com.sirithree.shopops.admin.model.service.impl;

import com.sirithree.shopops.admin.model.domain.ModelCallLogDto;
import com.sirithree.shopops.admin.model.domain.ModelCallLogQueryParam;
import com.sirithree.shopops.admin.model.domain.ModelCallStatus;
import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;
import com.sirithree.shopops.admin.model.config.ModelGatewayResilienceProperties;
import com.sirithree.shopops.admin.model.service.ModelCallLogStore;
import com.sirithree.shopops.admin.model.service.ModelGatewayService;
import com.sirithree.shopops.admin.model.service.ModelProviderClient;
import com.sirithree.shopops.admin.model.service.PromptTemplateService;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DefaultModelGatewayService implements ModelGatewayService {
    private final ModelCallLogStore callLogStore;
    private final ModelGatewayResilienceProperties resilienceProperties;
    private final PromptTemplateService promptTemplateService;
    private final Map<String, ModelProviderClient> providers;

    public DefaultModelGatewayService(ModelCallLogStore callLogStore,
                                      ModelGatewayResilienceProperties resilienceProperties,
                                      PromptTemplateService promptTemplateService,
                                      List<ModelProviderClient> providerClients) {
        this.callLogStore = callLogStore;
        this.resilienceProperties = resilienceProperties;
        this.promptTemplateService = promptTemplateService;
        this.providers = providerClients.stream()
                .collect(java.util.stream.Collectors.toMap(
                        client -> client.providerCode().toLowerCase(Locale.ROOT),
                        client -> client
                ));
    }

    @Override
    public ModelInvokeResult invoke(Long tenantId, Long shopId, Long userId, String username, ModelInvokeParam param) {
        long started = System.nanoTime();
        param = promptTemplateService.renderForInvoke(tenantId, param);
        String providerCode = defaultString(param.getProviderCode(), "echo").toLowerCase(Locale.ROOT);
        ModelProviderClient provider = providers.get(providerCode);
        if (provider == null) {
            ModelInvokeResult result = failedResult(providerCode, param.getModelName(), "PROVIDER_NOT_FOUND",
                    "模型供应商不存在: " + providerCode, started);
            persistLog(tenantId, shopId, userId, username, param, result);
            return result;
        }

        ModelInvokeResult result;
        try {
            result = provider.invoke(param);
            result.setStatus(ModelCallStatus.SUCCESS);
        } catch (RuntimeException ex) {
            result = fallbackResult(providerCode, param, ex);
            if (result == null) {
                result = failedResult(providerCode, param.getModelName(), "MODEL_INVOKE_FAILED", ex.getMessage(), started);
            }
        }
        ModelProviderClient resultProvider = providers.getOrDefault(result.getProviderCode().toLowerCase(Locale.ROOT), provider);
        result.setProviderCode(resultProvider.providerCode());
        result.setModelName(defaultString(result.getModelName(), defaultString(param.getModelName(), resultProvider.defaultModelName())));
        fillUsage(result, param.getPrompt(), result.getOutputText());
        result.setLatencyMs(elapsedMs(started));
        persistLog(tenantId, shopId, userId, username, param, result);
        return result;
    }

    @Override
    public CommonPage<ModelCallLogDto> listLogs(Long tenantId, Long shopId, ModelCallLogQueryParam queryParam) {
        return callLogStore.list(tenantId, shopId, queryParam);
    }

    private void persistLog(Long tenantId, Long shopId, Long userId, String username, ModelInvokeParam param, ModelInvokeResult result) {
        ModelCallLogDto saved = callLogStore.save(logOf(tenantId, shopId, userId, username, param, result));
        result.setCallId(saved.getCallId());
    }

    private ModelInvokeResult fallbackResult(String failedProviderCode, ModelInvokeParam param, RuntimeException failure) {
        if (!resilienceProperties.isFallbackEnabled()) {
            return null;
        }
        String fallbackProviderCode = defaultString(resilienceProperties.getFallbackProviderCode(), "echo").toLowerCase(Locale.ROOT);
        if (fallbackProviderCode.equals(failedProviderCode)) {
            return null;
        }
        ModelProviderClient fallbackProvider = providers.get(fallbackProviderCode);
        if (fallbackProvider == null) {
            return null;
        }
        ModelInvokeResult result = fallbackProvider.invoke(param);
        result.setStatus(ModelCallStatus.SUCCESS);
        result.setProviderCode(fallbackProvider.providerCode());
        result.setModelName(fallbackProvider.defaultModelName());
        result.setErrorCode(null);
        result.setErrorMessage(null);
        return result;
    }

    private ModelInvokeResult failedResult(String providerCode, String modelName, String errorCode,
                                           String errorMessage, long started) {
        ModelInvokeResult result = new ModelInvokeResult();
        result.setProviderCode(providerCode);
        result.setModelName(defaultString(modelName, "unknown"));
        result.setStatus(ModelCallStatus.FAILURE);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        result.setLatencyMs(elapsedMs(started));
        fillUsage(result, "", "");
        return result;
    }

    private ModelCallLogDto logOf(Long tenantId, Long shopId, Long userId, String username,
                                  ModelInvokeParam param, ModelInvokeResult result) {
        ModelCallLogDto log = new ModelCallLogDto();
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
        Integer promptTokens = result.getPromptTokens();
        Integer completionTokens = result.getCompletionTokens();
        if (promptTokens == null) {
            promptTokens = estimateTokens(prompt);
            result.setPromptTokens(promptTokens);
        }
        if (completionTokens == null) {
            completionTokens = estimateTokens(output);
            result.setCompletionTokens(completionTokens);
        }
        if (result.getTotalTokens() == null) {
            result.setTotalTokens(promptTokens + completionTokens);
        }
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

    private long elapsedMs(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000);
    }
}
