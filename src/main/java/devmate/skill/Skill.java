package devmate.skill;

import com.fasterxml.jackson.databind.JsonNode;
import devmate.util.JsonSchemaValidator;
import devmate.util.Result;

/**
 * Skill 接口 - MCP 标准化原子能力
 * 
 * Skill 是单一、确定、无自主决策的原子操作单元。
 * 每个 Skill 都有明确的输入输出，可测试、可监控、可复用。
 */
public interface Skill {

    /**
     * Skill 名称（给 LLM 识别使用）
     * 
     * @return Skill 名称，建议使用 snake_case 格式
     */
    String name();

    /**
     * Skill 描述（给 LLM 理解功能使用）
     * 
     * @return Skill 功能描述
     */
    String description();

    /**
     * 输入参数的 JSON Schema
     * 
     * @return JSON Schema 节点
     */
    JsonNode inputSchema();

    /**
     * 参数校验
     * 
     * @param input Skill 输入
     * @return 校验结果
     */
    default Result<Void> validate(SkillInput input) {
        return JsonSchemaValidator.validate(input.toJson(), inputSchema());
    }

    /**
     * 执行 Skill
     * 
     * @param input Skill 输入
     * @return 执行结果
     */
    Result<SkillResult> execute(SkillInput input);

    /**
     * 是否需要用户确认（危险操作）
     * 
     * @return true 表示执行前需要用户确认
     */
    default boolean requiresConfirmation() {
        return false;
    }

    /**
     * 执行超时时间（毫秒）
     * 
     * @return 超时时间，默认 30 秒
     */
    default long timeout() {
        return 30_000;
    }

    /**
     * Skill 类别（用于分组）
     * 
     * @return 类别名称
     */
    default String category() {
        return "general";
    }

    /**
     * Skill 版本
     * 
     * @return 版本号
     */
    default String version() {
        return "1.0.0";
    }

    /**
     * 是否启用
     * 
     * @return true 表示启用
     */
    default boolean isEnabled() {
        return true;
    }
}
