package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolGovernanceException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ToolInputSchemaValidator {
    private final JacksonJsonSupport jsonSupport;

    public ToolInputSchemaValidator(JacksonJsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    @SuppressWarnings("unchecked")
    public void validate(McpToolDto tool, Object input) {
        if (tool.getInputSchema() == null || tool.getInputSchema().isBlank()) {
            return;
        }
        Map<String, Object> schema = jsonSupport.toMap(tool.getInputSchema());
        Map<String, Object> values = jsonSupport.toMap(jsonSupport.toJson(input));
        Object requiredValue = schema.get("required");
        if (requiredValue instanceof Collection<?> required) {
            for (Object field : required) {
                String name = String.valueOf(field);
                if (!values.containsKey(name) || values.get(name) == null) {
                    throw invalid("Missing required field: " + name);
                }
            }
        }
        Map<String, Object> properties = schema.get("properties") instanceof Map<?, ?> map
                ? (Map<String, Object>) map : Map.of();
        if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
            Set<String> allowed = properties.keySet();
            Set<String> unexpected = values.keySet().stream()
                    .filter(name -> !allowed.contains(name))
                    .collect(Collectors.toSet());
            if (!unexpected.isEmpty()) {
                throw invalid("Unexpected fields: " + unexpected);
            }
        }
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object propertyDefinition = properties.get(entry.getKey());
            if (!(propertyDefinition instanceof Map<?, ?> definition) || entry.getValue() == null) {
                continue;
            }
            Object type = definition.get("type");
            if ("integer".equals(type) && !(entry.getValue() instanceof Byte
                    || entry.getValue() instanceof Short
                    || entry.getValue() instanceof Integer
                    || entry.getValue() instanceof Long)) {
                throw invalid("Field must be an integer: " + entry.getKey());
            }
            if ("string".equals(type) && !(entry.getValue() instanceof String)) {
                throw invalid("Field must be a string: " + entry.getKey());
            }
            if (entry.getValue() instanceof Number number) {
                validateNumericBoundary(entry.getKey(), number, definition);
            }
            if ("date".equals(definition.get("format")) && entry.getValue() instanceof String text) {
                try {
                    LocalDate.parse(text);
                } catch (DateTimeParseException ex) {
                    throw invalid("Field must use ISO-8601 date format: " + entry.getKey());
                }
            }
        }
    }


    private void validateNumericBoundary(String field, Number number, Map<?, ?> definition) {
        double value = number.doubleValue();
        Object minimum = definition.get("minimum");
        if (minimum instanceof Number min && value < min.doubleValue()) {
            throw invalid("Field is below minimum: " + field);
        }
        Object maximum = definition.get("maximum");
        if (maximum instanceof Number max && value > max.doubleValue()) {
            throw invalid("Field exceeds maximum: " + field);
        }
    }

    private ToolGovernanceException invalid(String message) {
        return new ToolGovernanceException("MCP_INPUT_INVALID", message);
    }
}
