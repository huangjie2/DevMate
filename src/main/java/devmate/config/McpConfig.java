package devmate.config;

import java.util.List;
import java.util.Map;

/**
 * MCP Configuration - Parsed from .mcp.json
 */
public record McpConfig(
    /**
     * MCP server configurations
     */
    Map<String, McpServer> mcpServers,

    /**
     * List of allowed paths
     */
    List<String> allowedPaths,

    /**
     * Maximum concurrent Skills
     */
    int maxConcurrentSkills,

    /**
     * Skill execution timeout (milliseconds)
     */
    long skillTimeout
) {

    /**
     * MCP Server Configuration
     */
    public record McpServer(
        /**
         * Startup command
         */
        String command,

        /**
         * Command arguments
         */
        List<String> args,

        /**
         * Environment variables
         */
        Map<String, String> env,

        /**
         * Description
         */
        String description,

        /**
         * Whether enabled
         */
        boolean enabled
    ) {
        public McpServer {
            args = args != null ? args : List.of();
            env = env != null ? env : Map.of();
            enabled = enabled != false; // Default enabled
        }
    }

    /**
     * Create default configuration
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
     * Builder
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