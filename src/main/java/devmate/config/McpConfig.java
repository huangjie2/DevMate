package devmate.config;

import java.util.List;
import java.util.Map;

/**
 * MCP 配置 - 从 .mcp.json 解析
 */
public record McpConfig(
    /**
     * MCP 服务器配置
     */
    Map<String, McpServer> mcpServers,

    /**
     * 允许访问的路径列表
     */
    List<String> allowedPaths,

    /**
     * 最大并发 Skill 数
     */
    int maxConcurrentSkills,

    /**
     * Skill 执行超时（毫秒）
     */
    long skillTimeout
) {

    /**
     * MCP 服务器配置
     */
    public record McpServer(
        /**
         * 启动命令
         */
        String command,

        /**
         * 命令参数
         */
        List<String> args,

        /**
         * 环境变量
         */
        Map<String, String> env,

        /**
         * 描述
         */
        String description,

        /**
         * 是否启用
         */
        boolean enabled
    ) {
        public McpServer {
            args = args != null ? args : List.of();
            env = env != null ? env : Map.of();
            enabled = enabled != false; // 默认启用
        }
    }

    /**
     * 创建默认配置
     */
    public static McpConfig defaultConfig() {
        return new McpConfig(
            Map.of(),
            List.of(
                System.getProperty("user.dir"),
                System.getProperty("user.home") + "/.config/devmate"
            ),
            3,
            30_000
        );
    }

    /**
     * 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Map<String, McpServer> mcpServers = Map.of();
        private List<String> allowedPaths = List.of(System.getProperty("user.dir"));
        private int maxConcurrentSkills = 3;
        private long skillTimeout = 30_000;

        public Builder mcpServers(Map<String, McpServer> mcpServers) {
            this.mcpServers = mcpServers;
            return this;
        }

        public Builder allowedPaths(List<String> allowedPaths) {
            this.allowedPaths = allowedPaths;
            return this;
        }

        public Builder maxConcurrentSkills(int maxConcurrentSkills) {
            this.maxConcurrentSkills = maxConcurrentSkills;
            return this;
        }

        public Builder skillTimeout(long skillTimeout) {
            this.skillTimeout = skillTimeout;
            return this;
        }

        public McpConfig build() {
            return new McpConfig(mcpServers, allowedPaths, maxConcurrentSkills, skillTimeout);
        }
    }
}
