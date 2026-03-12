package devmate.config;

import devmate.util.JsonMapper;
import devmate.util.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 配置加载器
 * 
 * 负责加载 .claude.md、.agent.md、.mcp.json 配置文件
 */
@ApplicationScoped
public class ConfigLoader {

    private static final String CLAUDE_CONFIG_FILE = ".claude.md";
    private static final String AGENT_CONFIG_FILE = ".agent.md";
    private static final String MCP_CONFIG_FILE = ".mcp.json";

    // Markdown 解析正则
    private static final Pattern SECTION_PATTERN = Pattern.compile("^##\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^[-*]\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^[-*]\\s+(\\S+):\\s*(.+)$", Pattern.MULTILINE);

    private Path projectRoot;

    /**
     * 设置项目根目录
     */
    public void setProjectRoot(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    /**
     * 获取项目根目录
     */
    public Path getProjectRoot() {
        if (projectRoot == null) {
            projectRoot = Path.of(System.getProperty("user.dir"));
        }
        return projectRoot;
    }

    /**
     * 加载项目配置 (.claude.md)
     */
    public Optional<ProjectConfig> loadClaudeConfig() {
        return loadClaudeConfig(getProjectRoot());
    }

    public Optional<ProjectConfig> loadClaudeConfig(Path root) {
        Path configPath = root.resolve(CLAUDE_CONFIG_FILE);
        if (!Files.exists(configPath)) {
            Log.debugf("Claude config not found: %s", configPath);
            return Optional.empty();
        }

        try {
            String content = Files.readString(configPath);
            ProjectConfig config = parseClaudeConfig(content);
            Log.infof("Loaded Claude config from: %s", configPath);
            return Optional.of(config);
        } catch (IOException e) {
            Log.errorf(e, "Failed to load Claude config: %s", configPath);
            return Optional.empty();
        }
    }

    /**
     * 加载 Agent 配置 (.agent.md)
     */
    public Optional<AgentConfig> loadAgentConfig() {
        return loadAgentConfig(getProjectRoot());
    }

    public Optional<AgentConfig> loadAgentConfig(Path root) {
        Path configPath = root.resolve(AGENT_CONFIG_FILE);
        if (!Files.exists(configPath)) {
            Log.debugf("Agent config not found: %s", configPath);
            return Optional.empty();
        }

        try {
            String content = Files.readString(configPath);
            AgentConfig config = parseAgentConfig(content);
            Log.infof("Loaded Agent config from: %s", configPath);
            return Optional.of(config);
        } catch (IOException e) {
            Log.errorf(e, "Failed to load Agent config: %s", configPath);
            return Optional.empty();
        }
    }

    /**
     * 加载 MCP 配置 (.mcp.json)
     */
    public Optional<McpConfig> loadMcpConfig() {
        return loadMcpConfig(getProjectRoot());
    }

    public Optional<McpConfig> loadMcpConfig(Path root) {
        Path configPath = root.resolve(MCP_CONFIG_FILE);
        if (!Files.exists(configPath)) {
            Log.debugf("MCP config not found: %s", configPath);
            return Optional.empty();
        }

        try {
            String content = Files.readString(configPath);
            McpConfig config = parseMcpConfig(content);
            Log.infof("Loaded MCP config from: %s", configPath);
            return Optional.of(config);
        } catch (IOException e) {
            Log.errorf(e, "Failed to load MCP config: %s", configPath);
            return Optional.empty();
        }
    }

    // ========== 解析方法 ==========

    private ProjectConfig parseClaudeConfig(String content) {
        Map<String, String> sections = parseSections(content);

        ProjectConfig.Builder builder = ProjectConfig.builder()
            .rawContent(content);

        // 解析项目信息
        String projectInfo = sections.get("项目信息");
        if (projectInfo != null) {
            parseKeyValuePairs(projectInfo).forEach((k, v) -> {
                switch (k) {
                    case "名称" -> builder.name(v);
                    case "类型" -> builder.type(v);
                }
            });
        }

        // 解析技术栈
        String techStack = sections.get("技术栈");
        if (techStack != null) {
            builder.techStack(parseListItems(techStack));
        }

        // 解析开发约束
        String constraints = sections.get("开发约束");
        if (constraints != null) {
            builder.constraints(parseListItems(constraints));
        }

        // 解析测试要求
        String testRequirements = sections.get("测试要求");
        if (testRequirements != null) {
            builder.testRequirements(parseListItems(testRequirements));
        }

        return builder.build();
    }

    private AgentConfig parseAgentConfig(String content) {
        Map<String, String> sections = parseSections(content);

        AgentConfig.Builder builder = AgentConfig.builder()
            .rawContent(content);

        // 解析角色定义
        String role = sections.get("角色定义");
        if (role != null) {
            builder.role(role.trim());
        }

        // 解析工作原则
        String principles = sections.get("工作原则");
        if (principles != null) {
            builder.principles(parseListItems(principles));
        }

        // 解析禁止操作
        String prohibited = sections.get("禁止操作");
        if (prohibited != null) {
            builder.prohibitedActions(parseListItems(prohibited));
        }

        return builder.build();
    }

    private McpConfig parseMcpConfig(String content) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> json = JsonMapper.fromJson(content, Map.class);

            McpConfig.Builder builder = McpConfig.builder();

            // 解析 MCP 服务器
            @SuppressWarnings("unchecked")
            Map<String, Object> servers = (Map<String, Object>) json.get("mcpServers");
            if (servers != null) {
                Map<String, McpConfig.McpServer> serverConfigs = servers.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> parseMcpServer((Map<String, Object>) e.getValue())
                    ));
                builder.mcpServers(serverConfigs);
            }

            // 解析允许的路径
            @SuppressWarnings("unchecked")
            List<String> allowedPaths = (List<String>) json.get("allowedPaths");
            if (allowedPaths != null) {
                builder.allowedPaths(resolvePaths(allowedPaths));
            }

            // 解析其他配置
            if (json.get("maxConcurrentSkills") != null) {
                builder.maxConcurrentSkills(((Number) json.get("maxConcurrentSkills")).intValue());
            }
            if (json.get("skillTimeout") != null) {
                builder.skillTimeout(((Number) json.get("skillTimeout")).longValue());
            }

            return builder.build();
        } catch (Exception e) {
            Log.errorf(e, "Failed to parse MCP config");
            return McpConfig.defaultConfig();
        }
    }

    @SuppressWarnings("unchecked")
    private McpConfig.McpServer parseMcpServer(Map<String, Object> json) {
        return new McpConfig.McpServer(
            (String) json.get("command"),
            (List<String>) json.get("args"),
            (Map<String, String>) json.get("env"),
            (String) json.get("description"),
            json.get("enabled") == null || Boolean.TRUE.equals(json.get("enabled"))
        );
    }

    private List<String> resolvePaths(List<String> paths) {
        Path root = getProjectRoot();
        return paths.stream()
            .map(p -> p.replace("${PROJECT_ROOT}", root.toString()))
            .map(p -> p.replace("${HOME}", System.getProperty("user.home")))
            .collect(java.util.stream.Collectors.toList());
    }

    // ========== Markdown 解析工具 ==========

    private Map<String, String> parseSections(String markdown) {
        java.util.Map<String, String> sections = new java.util.LinkedHashMap<>();
        String[] lines = markdown.split("\n");

        String currentSection = null;
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            Matcher matcher = SECTION_PATTERN.matcher(line);
            if (matcher.matches()) {
                // 保存上一个 section
                if (currentSection != null) {
                    sections.put(currentSection, currentContent.toString().trim());
                }
                currentSection = matcher.group(1);
                currentContent = new StringBuilder();
            } else if (currentSection != null) {
                currentContent.append(line).append("\n");
            }
        }

        // 保存最后一个 section
        if (currentSection != null) {
            sections.put(currentSection, currentContent.toString().trim());
        }

        return sections;
    }

    private List<String> parseListItems(String content) {
        List<String> items = new ArrayList<>();
        Matcher matcher = LIST_ITEM_PATTERN.matcher(content);
        while (matcher.find()) {
            items.add(matcher.group(1));
        }
        return items;
    }

    private Map<String, String> parseKeyValuePairs(String content) {
        Map<String, String> pairs = new java.util.LinkedHashMap<>();
        Matcher matcher = KEY_VALUE_PATTERN.matcher(content);
        while (matcher.find()) {
            pairs.put(matcher.group(1), matcher.group(2));
        }
        return pairs;
    }
}
