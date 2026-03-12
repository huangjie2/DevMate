package devmate.skill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import devmate.util.JsonMapper;
import devmate.util.JsonSchemaValidator;
import devmate.util.Result;

import java.util.Map;

/**
 * Skill 输入封装
 */
public record SkillInput(Map<String, Object> params) {

    private static final JsonNodeFactory JSON_NODE_FACTORY = JsonNodeFactory.instance;

    /**
     * 获取字符串参数
     */
    public String getString(String key) {
        Object value = params.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * 获取字符串参数，带默认值
     */
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取整数参数
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
     * 获取整数参数，带默认值
     */
    public int getInteger(String key, int defaultValue) {
        Integer value = getInteger(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取长整数参数
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
     * 获取布尔参数
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
     * 获取布尔参数，带默认值
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 检查是否包含参数
     */
    public boolean has(String key) {
        return params.containsKey(key);
    }

    /**
     * 转换为 JSON 节点
     */
    public JsonNode toJson() {
        return JsonMapper.toJsonNode(params);
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构建器
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
