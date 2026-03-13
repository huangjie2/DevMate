package devmate.config;

import java.util.List;
import java.util.Map;

/**
 * Agent Configuration - Parsed from .agent.md
 */
public record AgentConfig(
    /**
     * Role definition
     */
    String role,

    /**
     * Working principles
     */
    List<String> principles,

    /**
     * Prohibited actions
     */
    List<String> prohibitedActions,

    /**
     * Skill usage rules
     */
    Map<String, SkillRule> skillRules,

    /**
     * Error handling configuration
     */
    ErrorHandling errorHandling,

    /**
     * Context management configuration
     */
    ContextManagement contextManagement,

    /**
     * Raw Markdown content
     */
    String rawContent
) {

    /**
     * Skill rule
     */
    public record SkillRule(
        List<String> allowedPaths,
        List<String> allowedCommands,
        long defaultTimeout
    ) {}

    /**
     * Error handling configuration
     */
    public record ErrorHandling(
        boolean stopOnError,
        boolean explainError,
        boolean suggestSolution
    ) {
        public static ErrorHandling defaultConfig() {
            return new ErrorHandling(true, true, true);
        }
    }

    /**
     * Context management configuration
     */
    public record ContextManagement(
        int maxHistoryMessages,
        boolean autoSummarize,
        String persistencePath
    ) {
        public static ContextManagement defaultConfig() {
            return new ContextManagement(20, true, ".agent_context.json");
        }
    }

    /**
     * Create default configuration
     */
    public static AgentConfig defaultConfig() {
        return new AgentConfig(
            "You are a professional development assistant skilled in coding, project building, and debugging.",
            List.of(
                "Safety first: All dangerous operations require user confirmation",
                "Least privilege: Only access necessary files and directories",
                "Transparent and controllable: Explain each step to the user",
                "Incremental execution: Prefer low-risk operations"
            ),
            List.of(
                "Do not delete .git directory",
                "Do not modify system files",
                "Do not execute dangerous commands like rm -rf /",
                "Do not access files outside project directory"
            ),
            Map.of(),
            ErrorHandling.defaultConfig(),
            ContextManagement.defaultConfig(),
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
        private String role = "You are a professional development assistant skilled in coding, project building, and debugging.";
        private List<String> principles = List.of();
        private List<String> prohibitedActions = List.of();
        private Map<String, SkillRule> skillRules = Map.of();
        private ErrorHandling errorHandling = ErrorHandling.defaultConfig();
        private ContextManagement contextManagement = ContextManagement.defaultConfig();
        private String rawContent = "";

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder principles(List<String> principles) {
            this.principles = principles;
            return this;
        }

        public Builder prohibitedActions(List<String> prohibitedActions) {
            this.prohibitedActions = prohibitedActions;
            return this;
        }

        public Builder skillRules(Map<String, SkillRule> skillRules) {
            this.skillRules = skillRules;
            return this;
        }

        public Builder errorHandling(ErrorHandling errorHandling) {
            this.errorHandling = errorHandling;
            return this;
        }

        public Builder contextManagement(ContextManagement contextManagement) {
            this.contextManagement = contextManagement;
            return this;
        }

        public Builder rawContent(String rawContent) {
            this.rawContent = rawContent;
            return this;
        }

        public AgentConfig build() {
            return new AgentConfig(role, principles, prohibitedActions, skillRules, errorHandling, contextManagement, rawContent);
        }
    }
}