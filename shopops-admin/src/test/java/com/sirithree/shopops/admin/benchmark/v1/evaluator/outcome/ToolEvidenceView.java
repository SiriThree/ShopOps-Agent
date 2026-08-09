package com.sirithree.shopops.admin.benchmark.v1.evaluator.outcome;

import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ToolEvidenceView {
    private ToolEvidenceView() {}

    static Map<String, Object> successfulOutput(CollectedEvidence evidence, String toolCode) {
        for (Map<String, Object> log : evidence.toolLogs) {
            if (toolCode.equals(string(log.get("toolCode"))) && "SUCCESS".equalsIgnoreCase(string(log.get("status")))) {
                return map(log.get("output"));
            }
        }
        return Map.of();
    }

    static Map<String, Object> successfulInput(CollectedEvidence evidence, String toolCode) {
        for (Map<String, Object> log : evidence.toolLogs) {
            if (toolCode.equals(string(log.get("toolCode"))) && "SUCCESS".equalsIgnoreCase(string(log.get("status")))) {
                return map(log.get("input"));
            }
        }
        return Map.of();
    }

    static List<Map<String, Object>> logs(CollectedEvidence evidence, String toolCode) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> log : evidence.toolLogs) {
            if (toolCode.equals(string(log.get("toolCode")))) result.add(log);
        }
        return result;
    }

    static Set<String> successfulTools(CollectedEvidence evidence) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> log : evidence.toolLogs) {
            if ("SUCCESS".equalsIgnoreCase(string(log.get("status"))) && log.get("toolCode") != null) {
                result.add(String.valueOf(log.get("toolCode")));
            }
        }
        return result;
    }

    static Set<String> failedTools(CollectedEvidence evidence) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> log : evidence.toolLogs) {
            if ("FAILED".equalsIgnoreCase(string(log.get("status"))) && log.get("toolCode") != null) {
                result.add(String.valueOf(log.get("toolCode")));
            }
        }
        return result;
    }

    static String errorCode(Map<String, Object> log) {
        return string(log.get("errorCode"));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    static List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> raw) result.add(map(raw));
        }
        return result;
    }

    static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
