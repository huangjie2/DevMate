package devmate.config;

import java.util.List;
import java.util.Map;

/**
 * 项目配置 - 从 .claude.md 解析
 */
public record ProjectConfig(
    /**
     * 项目名称
     */
    String name,

    /**
     * 项目类型
     */
    String type,

    /**
     * 技术栈
     */
    List<String> techStack,

    /**
     * 项目结构
     */
    Map<String, String> structure,

    /**
     * 开发约束
     */
    List<String> constraints,

    /**
     * 测试要求
     */
    List<String> testRequirements,

    /**
     * 原始 Markdown 内容
     */
    String rawContent
) {

    /**
     * 创建默认配置
     */
    public static ProjectConfig defaultConfig() {
        return new ProjectConfig(
            "Unknown Project",
            "unknown",
            List.of(),
            Map.of(),
            List.of(),
            List.of(),
            ""
        );
    }

    /**
     * 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "Unknown Project";
        private String type = "unknown";
        private List<String> techStack = List.of();
        private Map<String, String> structure = Map.of();
        private List<String> constraints = List.of();
        private List<String> testRequirements = List.of();
        private String rawContent = "";

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder techStack(List<String> techStack) {
            this.techStack = techStack;
            return this;
        }

        public Builder structure(Map<String, String> structure) {
            this.structure = structure;
            return this;
        }

        public Builder constraints(List<String> constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder testRequirements(List<String> testRequirements) {
            this.testRequirements = testRequirements;
            return this;
        }

        public Builder rawContent(String rawContent) {
            this.rawContent = rawContent;
            return this;
        }

        public ProjectConfig build() {
            return new ProjectConfig(name, type, techStack, structure, constraints, testRequirements, rawContent);
        }
    }
}
