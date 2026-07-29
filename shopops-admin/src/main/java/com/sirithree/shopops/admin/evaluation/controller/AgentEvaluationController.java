package com.sirithree.shopops.admin.evaluation.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.business.support.ConfiguredFilePathResolver;
import com.sirithree.shopops.common.api.CommonResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/evaluation")
public class AgentEvaluationController {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String naturalLanguageBatchSummaryFile;

    public AgentEvaluationController(ObjectMapper objectMapper,
                                     @Value("${shopops.evaluation.natural-language-batch.summary-file:docs/evaluation/agent-natural-language-batch-summary.json}")
                                     String naturalLanguageBatchSummaryFile) {
        this.objectMapper = objectMapper;
        this.naturalLanguageBatchSummaryFile = naturalLanguageBatchSummaryFile;
    }

    @GetMapping("/agent-natural-language-batch")
    @RequireRole(AuthRole.ADMIN)
    public CommonResult<Map<String, Object>> naturalLanguageBatchSummary() {
        Path path = ConfiguredFilePathResolver.resolve(naturalLanguageBatchSummaryFile);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("available", Files.isRegularFile(path) && Files.isReadable(path));
        result.put("summaryPath", path.toString());
        result.put("checkedAt", LocalDateTime.now().toString());
        if (!(Boolean) result.get("available")) {
            result.put("message", "Agent natural-language batch summary has not been generated yet.");
            return CommonResult.success(result);
        }

        try {
            Map<String, Object> summary = objectMapper.readValue(path.toFile(), MAP_TYPE);
            result.put("message", "Agent natural-language batch summary loaded.");
            result.put("summary", summary);
            return CommonResult.success(result);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read Agent natural-language batch summary: " + path, ex);
        }
    }
}
