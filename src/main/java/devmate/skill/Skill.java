package devmate.skill;

import com.fasterxml.jackson.databind.JsonNode;
import devmate.util.JsonSchemaValidator;
import devmate.util.Result;

/**
 * Skill Interface - MCP Standardized Atomic Capability
 * 
 * Skill is a single, deterministic, non-autonomous atomic operation unit.
 * Each Skill has clear input/output, testable, monitorable, and reusable.
 */
public interface Skill {

    /**
     * Skill name (for LLM identification)
     * 
     * @return Skill name, recommend snake_case format
     */
    String name();

    /**
     * Skill description (for LLM understanding)
     * 
     * @return Skill functionality description
     */
    String description();

    /**
     * Input parameter JSON Schema
     * 
     * @return JSON Schema node
     */
    JsonNode inputSchema();

    /**
     * Parameter validation
     * 
     * @param input Skill input
     * @return Validation result
     */
    default Result<Void> validate(SkillInput input) {
        return JsonSchemaValidator.validate(input.toJson(), inputSchema());
    }

    /**
     * Execute Skill
     * 
     * @param input Skill input
     * @return Execution result
     */
    Result<SkillResult> execute(SkillInput input);

    /**
     * Whether user confirmation is required (dangerous operations)
     * 
     * @return true means user confirmation is required before execution
     */
    default boolean requiresConfirmation() {
        return false;
    }

    /**
     * Execution timeout (milliseconds)
     * 
     * @return Timeout, default 30 seconds
     */
    default long timeout() {
        return 30_000;
    }

    /**
     * Skill category (for grouping)
     * 
     * @return Category name
     */
    default String category() {
        return "general";
    }

    /**
     * Skill version
     * 
     * @return Version number
     */
    default String version() {
        return "1.0.0";
    }

    /**
     * Whether enabled
     * 
     * @return true means enabled
     */
    default boolean isEnabled() {
        return true;
    }
}