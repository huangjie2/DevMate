package devmate.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import devmate.util.JsonMapper;
import devmate.util.JsonSchemaValidator;
import devmate.util.Result;

import java.util.Map;

/**
 * Skill Input Wrapper
 */
public record SkillInput(Map<String, Object> params) {

    private static final JsonNodeFactory JSON_NODE_FACTORY = JsonNodeFactory.instance;

    /**
     * Get string parameter
     */
    public String getString(String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Get string parameter with default value
     */
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get integer parameter
     */
    public Integer getInteger(String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get integer parameter with default value
     */
    public int getInteger(String key, int defaultValue) {
        Integer value = getInteger(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get long parameter
     */
    public Long getLong(String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Get boolean parameter
     */
    public Boolean getBoolean(String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Get boolean parameter with default value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Check if parameter exists
     */
    public boolean has(String key) {
        return params.containsKey(key);
    }

    /**
     * Convert to JSON node
     */
    public JsonNode toJson() {
        return JsonMapper.toJsonNode(params);
    }

    /**
     * Create builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder
     */
    public static class Builder {
        private final ObjectNode params = JSON_NODE_FACTORY.objectNode();

        public Builder put(String key, String value) {
            params.put(key, value);
            return this;
        }

        public Builder put(String key, int value) {
            params.put(key, value);
            return this;
        }

        public Builder put(String key, long value) {
            params.put(key, value);
            return this;
        }

        public Builder put(String key, boolean value) {
            params.put(key, value);
            return this;
        }

        public Builder put(String key, double value) {
            params.put(key, value);
            return this;
        }

        public Builder put(String key, JsonNode value) {
            params.set(key, value);
            return this;
        }

        public SkillInput build() {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = JsonMapper.fromJson(params.toString(), Map.class);
            return new SkillInput(map);
        }
    }
}