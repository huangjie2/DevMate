package devmate.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.quarkus.logging.Log;

import java.util.Map;

/**
 * JSON 工具类 - 基于 Jackson
 */
public final class JsonMapper {

    private static final ObjectMapper MAPPER = createMapper();

    private JsonMapper() {
        // 工具类，禁止实例化
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        return mapper;
    }

    /**
     * 获取底层 ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    // ========== 序列化 ==========

    /**
     * 对象转 JSON 字符串
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to serialize object to JSON: %s", obj);
            throw new JsonMappingException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * 对象转格式化的 JSON 字符串
     */
    public static String toPrettyJson(Object obj) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to serialize object to pretty JSON: %s", obj);
            throw new JsonMappingException("Failed to serialize object to pretty JSON", e);
        }
    }

    /**
     * 对象转 JsonNode
     */
    public static JsonNode toJsonNode(Object obj) {
        return MAPPER.valueToTree(obj);
    }

    // ========== 反序列化 ==========

    /**
     * JSON 字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to deserialize JSON to %s: %s", clazz.getName(), json);
            throw new JsonMappingException("Failed to deserialize JSON", e);
        }
    }

    /**
     * JSON 字符串转对象（支持泛型）
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to deserialize JSON to %s: %s", typeRef.getType(), json);
            throw new JsonMappingException("Failed to deserialize JSON", e);
        }
    }

    /**
     * JSON 字符串转 JsonNode
     */
    public static JsonNode fromJsonToNode(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            Log.errorf(e, "Failed to parse JSON: %s", json);
            throw new JsonMappingException("Failed to parse JSON", e);
        }
    }

    /**
     * JSON 字符串转 Map
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        return fromJson(json, new TypeReference<>() {});
    }

    // ========== 安全操作 ==========

    /**
     * 安全地转 JSON，失败返回 null
     */
    public static String toJsonSafe(Object obj) {
        try {
            return toJson(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 安全地反序列化，失败返回 null
     */
    public static <T> T fromJsonSafe(String json, Class<T> clazz) {
        try {
            return fromJson(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断字符串是否为有效 JSON
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        try {
            MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * JSON 映射异常
     */
    public static class JsonMappingException extends RuntimeException {
        public JsonMappingException(String message) {
            super(message);
        }

        public JsonMappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
