package devmate.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * JSON Schema 校验器
 * 基于 networknt/json-schema-validator 实现
 */
public final class JsonSchemaValidator {

    private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    private JsonSchemaValidator() {
        // 工具类，禁止实例化
    }

    /**
     * 校验 JSON 节点是否符合 Schema
     *
     * @param jsonNode 要校验的 JSON 节点
     * @param schema   JSON Schema
     * @return 校验结果
     */
    public static Result<Void> validate(JsonNode jsonNode, JsonNode schema) {
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setTypeLoose(false);
        
        JsonSchema jsonSchema = SCHEMA_FACTORY.getSchema(schema, config);
        Set<ValidationMessage> errors = jsonSchema.validate(jsonNode);

        if (errors.isEmpty()) {
            return Result.success(null);
        }

        String errorMessage = errors.stream()
            .map(ValidationMessage::getMessage)
            .collect(Collectors.joining("; "));

        return Result.failure("参数校验失败: " + errorMessage);
    }

    /**
     * 校验 JSON 字符串是否符合 Schema
     *
     * @param json       JSON 字符串
     * @param schemaJson Schema JSON 字符串
     * @return 校验结果
     */
    public static Result<Void> validate(String json, String schemaJson) {
        try {
            JsonNode jsonNode = JsonMapper.fromJsonToNode(json);
            JsonNode schemaNode = JsonMapper.fromJsonToNode(schemaJson);
            return validate(jsonNode, schemaNode);
        } catch (Exception e) {
            return Result.failure("JSON 解析失败: " + e.getMessage());
        }
    }

    /**
     * 校验对象是否符合 Schema
     *
     * @param object 要校验的对象
     * @param schema JSON Schema
     * @return 校验结果
     */
    public static Result<Void> validate(Object object, JsonNode schema) {
        try {
            JsonNode jsonNode = JsonMapper.toJsonNode(object);
            return validate(jsonNode, schema);
        } catch (Exception e) {
            return Result.failure("对象转 JSON 失败: " + e.getMessage());
        }
    }
}
