package devmate.skill;

import devmate.util.JsonMapper;

import java.util.Map;

/**
 * Skill 执行结果
 */
public record SkillResult(
    /**
     * 结果内容
     */
    String content,

    /**
     * 元数据（可选）
     */
    Map<String, Object> metadata
) {

    private static final Map<String, Object> EMPTY_METADATA = Map.of();

    /**
     * 创建简单结果（无元数据）
     */
    public SkillResult(String content) {
        this(content, EMPTY_METADATA);
    }

    /**
     * 创建带元数据的结果
     */
    public SkillResult(String content, Map<String, Object> metadata) {
        this.content = content;
        this.metadata = metadata != null ? metadata : EMPTY_METADATA;
    }

    /**
     * 创建成功结果
     */
    public static SkillResult of(String content) {
        return new SkillResult(content);
    }

    /**
     * 创建带元数据的成功结果
     */
    public static SkillResult of(String content, Map<String, Object> metadata) {
        return new SkillResult(content, metadata);
    }

    /**
     * 创建带单个元数据的成功结果
     */
    public static SkillResult of(String content, String key, Object value) {
        return new SkillResult(content, Map.of(key, value));
    }

    /**
     * 转换为 JSON 格式
     */
    public String toJson() {
        return JsonMapper.toJson(this);
    }

    @Override
    public String toString() {
        if (metadata.isEmpty()) {
            return content;
        }
        return String.format("%s\n[Metadata: %s]", content, JsonMapper.toPrettyJson(metadata));
    }
}
