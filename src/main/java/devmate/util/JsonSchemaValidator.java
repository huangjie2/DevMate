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
 * JSON Schema Validator
 * Based on networknt/json-schema-validator
 */
public final class JsonSchemaValidator {

    private static final JsonSchemaFactory SCHEMA_FACTORY = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    private JsonSchemaValidator() {
        // Utility class, prevent instantiation
    }

    /**
     * Validate JSON node against Schema
     *
     * @param jsonNode JSON node to validate
     * @param schema   JSON Schema
     * @return Validation result
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

        return Result.failure("Parameter validation failed: " + errorMessage);
    }

    /**
     * Validate JSON string against Schema
     *
     * @param json       JSON string
     * @param schemaJson Schema JSON string
     * @return Validation result
     */
    public static Result<Void> validate(String json, String schemaJson) {
        try {
            JsonNode jsonNode = JsonMapper.fromJsonToNode(json);
            JsonNode schemaNode = JsonMapper.fromJsonToNode(schemaJson);
            return validate(jsonNode, schemaNode);
        } catch (Exception e) {
            return Result.failure("JSON parsing failed: " + e.getMessage());
        }
    }

    /**
     * Validate object against Schema
     *
     * @param object Object to validate
     * @param schema JSON Schema
     * @return Validation result
     */
    public static Result<Void> validate(Object object, JsonNode schema) {
        try {
            JsonNode jsonNode = JsonMapper.toJsonNode(object);
            return validate(jsonNode, schema);
        } catch (Exception e) {
            return Result.failure("Failed to convert object to JSON: " + e.getMessage());
        }
    }
}