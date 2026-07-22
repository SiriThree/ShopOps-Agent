package com.sirithree.shopops.admin.model.service.impl;

import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.PromptEnableParam;
import com.sirithree.shopops.admin.model.domain.PromptRenderResult;
import com.sirithree.shopops.admin.model.domain.PromptRenderTestParam;
import com.sirithree.shopops.admin.model.domain.PromptTemplateDto;
import com.sirithree.shopops.admin.model.domain.PromptTemplateQueryParam;
import com.sirithree.shopops.admin.model.domain.PromptTemplateStatus;
import com.sirithree.shopops.admin.model.domain.PromptVersionParam;
import com.sirithree.shopops.admin.model.service.PromptTemplateService;
import com.sirithree.shopops.admin.model.service.PromptTemplateStore;
import com.sirithree.shopops.common.api.CommonPage;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class DefaultPromptTemplateService implements PromptTemplateService {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private final PromptTemplateStore promptTemplateStore;

    public DefaultPromptTemplateService(PromptTemplateStore promptTemplateStore) {
        this.promptTemplateStore = promptTemplateStore;
    }

    @Override
    public PromptTemplateDto createVersion(Long tenantId, Long userId, String promptCode, PromptVersionParam param) {
        if (promptTemplateStore.findByCodeAndVersion(tenantId, promptCode, param.getVersion()).isPresent()) {
            throw new IllegalArgumentException("Prompt 版本已存在: " + promptCode + "@" + param.getVersion());
        }
        PromptTemplateDto dto = new PromptTemplateDto();
        dto.setTenantId(tenantId);
        dto.setPromptCode(promptCode);
        dto.setPromptName(param.getPromptName());
        dto.setTaskType(param.getTaskType());
        dto.setTemplateContent(param.getTemplateContent());
        dto.setVersion(param.getVersion());
        dto.setStatus(Boolean.TRUE.equals(param.getActive()) ? PromptTemplateStatus.ACTIVE : PromptTemplateStatus.DRAFT);
        dto.setCreatedBy(userId);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        if (PromptTemplateStatus.ACTIVE.equals(dto.getStatus())) {
            promptTemplateStore.deactivateCode(tenantId, promptCode);
        }
        return promptTemplateStore.save(dto);
    }

    @Override
    public PromptTemplateDto enableVersion(Long tenantId, String promptCode, PromptEnableParam param) {
        promptTemplateStore.activateVersion(tenantId, promptCode, param.getVersion());
        return promptTemplateStore.findByCodeAndVersion(tenantId, promptCode, param.getVersion())
                .orElseThrow(() -> new IllegalArgumentException("Prompt 版本不存在: " + promptCode + "@" + param.getVersion()));
    }

    @Override
    public CommonPage<PromptTemplateDto> list(Long tenantId, PromptTemplateQueryParam query) {
        return promptTemplateStore.list(tenantId, query);
    }

    @Override
    public PromptTemplateDto get(Long tenantId, String promptCode, String version) {
        Optional<PromptTemplateDto> template = blank(version)
                ? promptTemplateStore.findActive(tenantId, promptCode)
                : promptTemplateStore.findByCodeAndVersion(tenantId, promptCode, version);
        return template.orElseThrow(() -> new IllegalArgumentException("Prompt 模板不存在: " + promptCode));
    }

    @Override
    public PromptRenderResult renderTest(Long tenantId, String promptCode, PromptRenderTestParam param) {
        PromptTemplateDto template = get(tenantId, promptCode, param.getVersion());
        PromptRenderResult result = new PromptRenderResult();
        result.setPromptCode(promptCode);
        result.setVersion(template.getVersion());
        result.setRenderedPrompt(render(template.getTemplateContent(), variablesOf(param.getPrompt(), param.getVariables())));
        return result;
    }

    @Override
    public ModelInvokeParam renderForInvoke(Long tenantId, ModelInvokeParam param) {
        if (blank(param.getPromptCode())) {
            return param;
        }
        Optional<PromptTemplateDto> templateOptional = blank(param.getPromptVersion())
                ? promptTemplateStore.findActive(tenantId, param.getPromptCode())
                : promptTemplateStore.findByCodeAndVersion(tenantId, param.getPromptCode(), param.getPromptVersion());
        if (templateOptional.isEmpty()) {
            return param;
        }
        PromptTemplateDto template = templateOptional.get();
        ModelInvokeParam rendered = copyOf(param);
        rendered.setPromptVersion(template.getVersion());
        rendered.setPrompt(render(template.getTemplateContent(), variablesOf(param.getPrompt(), param.getMetadata())));
        return rendered;
    }

    private String render(String template, Map<String, Object> variables) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Object value = variables.get(matcher.group(1));
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : value.toString()));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private Map<String, Object> variablesOf(String prompt, Map<String, Object> variables) {
        Map<String, Object> merged = new HashMap<>();
        if (variables != null) {
            merged.putAll(variables);
        }
        merged.put("prompt", prompt == null ? "" : prompt);
        return merged;
    }

    private ModelInvokeParam copyOf(ModelInvokeParam source) {
        ModelInvokeParam copy = new ModelInvokeParam();
        copy.setProviderCode(source.getProviderCode());
        copy.setModelName(source.getModelName());
        copy.setPrompt(source.getPrompt());
        copy.setPromptCode(source.getPromptCode());
        copy.setPromptVersion(source.getPromptVersion());
        copy.setTraceId(source.getTraceId());
        copy.setTaskId(source.getTaskId());
        copy.setReportId(source.getReportId());
        copy.setTimeoutMs(source.getTimeoutMs());
        copy.setMetadata(source.getMetadata());
        return copy;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
