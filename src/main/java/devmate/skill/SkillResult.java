package devmate.skill;

import devmate.util.JsonMapper;

import java.util.Map;

/**
 * Skill Execution Result
 */
public record SkillResult(
    /**
     * Result content
     */
    String content,

    /**
     * Metadata (optional)
     */
    Map<String, Object> metadata
) {

    private static final Map<String, Object> EMPTY_METADATA = Map.of();

    /**
     * Create simple result (no metadata)
     */
    public SkillResult(String content) {
        this(content, EMPTY_METADATA);
    }

    /**
     * Create result with metadata
     */
    public SkillResult(String content, Map<String, Object> metadata) {
        this.content = content;
        this.metadata = metadata != null ? metadata : EMPTY_METADATA;
    }

    /**
     * Create success result
     */
    public static SkillResult of(String content) {
        return new SkillResult(content);
    }

    /**
     * Create success result with metadata
     */
    public static SkillResult of(String content, Map<String, Object> metadata) {
        return new SkillResult(content, metadata);
    }

    /**
     * Create success result with single metadata
     */
    public static SkillResult of(String content, String key, Object value) {
        return new SkillResult(content, Map.of(key, value));
    }

    /**
     * Convert to JSON format
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