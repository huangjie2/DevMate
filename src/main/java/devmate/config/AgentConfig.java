package devmate.config;

import java.util.List;
import java.util.Map;

/**
 * Agent 配置 - 从 .agent.md 解析
 */
public record AgentConfig(
    /**
     * 角色定义
     */
    String role,

    /**
     * 工作原则
     */
    List<String> principles,

    /**
     * 禁止操作
     */
    List<String> prohibitedActions,

    /**
     * Skill 使用规范
     */
    Map<String, SkillRule> skillRules,

    /**
     * 错误处理配置
     */
    ErrorHandling errorHandling,

    /**
     * 上下文管理配置
     */
    ContextManagement contextManagement,

    /**
     * 原始 Markdown 内容
     */
    String rawContent
) {

    /**
     * Skill 规则
     */
    public record SkillRule(
        List<String> allowedPaths,
        List<String> allowedCommands,
        long defaultTimeout
    ) {}

    /**
     * 错误处理配置
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
     * 上下文管理配置
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
     * 创建默认配置
     */
    public static AgentConfig defaultConfig() {
        return new AgentConfig(
            "你是一个专业的开发助手，擅长代码开发、项目构建和调试。",
            List.of(
                "安全第一：所有危险操作必须用户确认",
                "最小权限：只访问必要的文件和目录",
                "透明可控：每步操作都要向用户说明",
                "增量执行：优先选择风险小的操作"
            ),
            List.of(
                "不得删除 .git 目录",
                "不得修改系统文件",
                "不得执行 rm -rf / 等危险命令",
                "不得访问项目目录外的文件"
            ),
            Map.of(),
            ErrorHandling.defaultConfig(),
            ContextManagement.defaultConfig(),
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
        private String role = "你是一个专业的开发助手，擅长代码开发、项目构建和调试。";
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
