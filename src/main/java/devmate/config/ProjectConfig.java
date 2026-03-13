package devmate.config;

import java.util.List;
import java.util.Map;

/**
 * Project Configuration - Parsed from .claude.md
 */
public record ProjectConfig(
    /**
     * Project name
     */
    String name,

    /**
     * Project type
     */
    String type,

    /**
     * Tech stack
     */
    List<String> techStack,

    /**
     * Project structure
     */
    Map<String, String> structure,

    /**
     * Development constraints
     */
    List<String> constraints,

    /**
     * Test requirements
     */
    List<String> testRequirements,

    /**
     * Raw Markdown content
     */
    String rawContent
) {

    /**
     * Create default configuration
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
     * Builder
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