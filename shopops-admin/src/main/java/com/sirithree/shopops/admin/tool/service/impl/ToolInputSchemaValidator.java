package com.sirithree.shopops.admin.tool.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.tool.domain.McpToolDto;
import com.sirithree.shopops.admin.tool.domain.ToolGovernanceException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
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
        if (tool.getInputSchema() == null || tool.getInputSchema().isBlank()) return;
        Map<String, Object> schema = jsonSupport.toMap(tool.getInputSchema());
        Object value = input == null ? Map.of() : jsonSupport.toMap(jsonSupport.toJson(input));
        validateValue("$", value, schema);
    }

    @SuppressWarnings("unchecked")
    private void validateValue(String path, Object value, Map<?, ?> schema) {
        validateEnum(path, value, schema);
        String type = schema.get("type") == null ? null : String.valueOf(schema.get("type"));
        if (type == null || type.isBlank()) {
            if (schema.containsKey("properties") && value instanceof Map<?, ?> map) validateObject(path, map, schema);
            return;
        }
        switch (type) {
            case "object" -> {
                if (!(value instanceof Map<?, ?> map)) throw invalid("Field must be an object: " + path);
                validateObject(path, map, schema);
            }
            case "array" -> {
                if (!(value instanceof Collection<?> collection)) throw invalid("Field must be an array: " + path);
                Object items = schema.get("items");
                if (items instanceof Map<?, ?> itemSchema) {
                    int index = 0;
                    for (Object item : collection) validateValue(path + "[" + index++ + "]", item, itemSchema);
                }
            }
            case "integer" -> {
                if (!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long))
                    throw invalid("Field must be an integer: " + path);
                validateNumericBoundary(path, (Number) value, schema);
            }
            case "number" -> {
                if (!(value instanceof Number number)) throw invalid("Field must be a number: " + path);
                validateNumericBoundary(path, number, schema);
            }
            case "string" -> {
                if (!(value instanceof String text)) throw invalid("Field must be a string: " + path);
                validateString(path, text, schema);
            }
            case "boolean" -> {
                if (!(value instanceof Boolean)) throw invalid("Field must be a boolean: " + path);
            }
            default -> { }
        }
    }

    @SuppressWarnings("unchecked")
    private void validateObject(String path, Map<?, ?> values, Map<?, ?> schema) {
        Object requiredValue = schema.get("required");
        if (requiredValue instanceof Collection<?> required) {
            for (Object field : required) {
                String name = String.valueOf(field);
                if (!values.containsKey(name) || values.get(name) == null)
                    throw invalid("Missing required field: " + child(path, name));
            }
        }
        Map<String, Object> properties = schema.get("properties") instanceof Map<?, ?> map
                ? (Map<String, Object>) map : Map.of();
        if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
            Set<String> allowed = properties.keySet();
            Set<String> unexpected = values.keySet().stream().map(String::valueOf)
                    .filter(name -> !allowed.contains(name)).collect(Collectors.toSet());
            if (!unexpected.isEmpty()) throw invalid("Unexpected fields: " + unexpected);
        }
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            Object definition = properties.get(String.valueOf(entry.getKey()));
            if (definition instanceof Map<?, ?> propertySchema && entry.getValue() != null)
                validateValue(child(path, String.valueOf(entry.getKey())), entry.getValue(), propertySchema);
        }
    }

    private void validateEnum(String path, Object value, Map<?, ?> schema) {
        if (schema.get("enum") instanceof Collection<?> allowed && value != null && !allowed.contains(value))
            throw invalid("Field is not an allowed enum value: " + path);
    }

    private void validateString(String path, String text, Map<?, ?> schema) {
        Object minLength = schema.get("minLength");
        if (minLength instanceof Number min && text.length() < min.intValue())
            throw invalid("Field is shorter than minLength: " + path);
        Object maxLength = schema.get("maxLength");
        if (maxLength instanceof Number max && text.length() > max.intValue())
            throw invalid("Field exceeds maxLength: " + path);
        if ("date".equals(schema.get("format"))) {
            try { LocalDate.parse(text); }
            catch (DateTimeParseException ex) { throw invalid("Field must use ISO-8601 date format: " + path); }
        }
    }

    private void validateNumericBoundary(String field, Number number, Map<?, ?> definition) {
        double value = number.doubleValue();
        Object minimum = definition.get("minimum");
        if (minimum instanceof Number min && value < min.doubleValue()) throw invalid("Field is below minimum: " + field);
        Object maximum = definition.get("maximum");
        if (maximum instanceof Number max && value > max.doubleValue()) throw invalid("Field exceeds maximum: " + field);
    }

    private String child(String path, String name) { return "$".equals(path) ? "$." + name : path + "." + name; }
    private ToolGovernanceException invalid(String message) { return new ToolGovernanceException("MCP_INPUT_INVALID", message); }
}
