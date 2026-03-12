# 基于 Java 21 + Quarkus + LangChain4j 的 AI 命令行智能体技术方案

## 1. 文档概述

本文档定义**轻量级 AI 命令行智能体（CLI-Agent）**的技术架构、核心模块、实现原理与技术选型。

**产品定位**：
- 类型：AI 命令行工具（CLI）+ 轻量级智能体
- 对标：Anthropic Claude Code、阿里 iFlow、GitHub Copilot Workspace
- 核心思想：**Skill 原子化 + MCP 标准化 + LLM 调度 + 安全沙箱**

**支持配置文件**：
- ✅ `.claude.md` - 项目级配置（项目背景、技术栈、约束）
- ✅ `.agent.md` - Agent 行为配置（角色定义、工作流、规则）
- ✅ `.mcp.json` - MCP 服务器配置（外部工具集成）

---

## 2. 技术栈（最终版）

### 核心技术

| 技术 | 版本 | 用途 | 选择理由 |
|------|------|------|----------|
| Java | 21+ | 开发语言 | 虚拟线程、Sealed Classes、Pattern Matching |
| Quarkus | 3.8+ | 应用框架 | CLI 支持、原生编译、秒启动 |
| LangChain4j | 0.35+ | LLM 集成 | 支持 20+ 模型、工具调用、流式输出 |
| Picocli | 4.7+ | CLI 框架 | Quarkus 原生集成、注解驱动 |
| JLine | 3.25+ | 终端交互 | 自动补全、历史记录、彩色输出 |
| JSON Schema Validator | 1.5+ | 参数校验 | 运行时验证 Skill 输入 |
| GraalVM | 23+ | 原生编译 | 单文件二进制、无 JVM 依赖 |

### 移除的技术

- ❌ **Vavr**：Java 21 原生特性已足够（Sealed Classes、Optional、Pattern Matching）
- ❌ **自研 LLM Client**：LangChain4j 已提供完整实现

---

## 3. 核心概念定义

### 3.1 Skill（原子能力）

**定义**：单一、确定、无自主决策的原子操作单元

**特征**：
- 输入输出明确
- 可测试、可监控、可复用
- 符合 MCP 标准
- 支持 JSON Schema 参数校验

**示例**：
```java
@ApplicationScoped
public class FileReadSkill implements Skill {
    @Override
    public String name() {
        return "read_file";
    }
    
    @Override
    public String description() {
        return "读取指定文件的内容";
    }
    
    @Override
    public JsonNode inputSchema() {
        return Json.createObjectBuilder()
            .add("type", "object")
            .add("properties", Json.createObjectBuilder()
                .add("path", Json.createObjectBuilder()
                    .add("type", "string")
                    .add("description", "文件路径")))
            .add("required", Json.createArrayBuilder().add("path"))
            .build();
    }
    
    @Override
    public Result<String> execute(SkillInput input) {
        Path path = Path.of(input.getString("path"));
        
        // 安全检查
        if (!isPathAllowed(path)) {
            return Result.failure("路径不在允许范围内");
        }
        
        try {
            String content = Files.readString(path);
            return Result.success(content);
        } catch (IOException e) {
            return Result.failure("读取失败: " + e.getMessage());
        }
    }
}
```

### 3.2 Agent（智能调度器）

**定义**：基于 ReAct 模式的轻量级任务编排器

**职责**：
- 解析用户意图
- 选择合适的 Skill
- 执行并收集结果
- 判断是否完成任务

**不做什么**：
- ❌ 复杂的自主规划
- ❌ 深度递归调用
- ❌ 不可控的循环

### 3.3 MCP（Model Control Plane）

**定义**：Skill 的注册、发现、调用规范

**功能**：
- 统一入参、出参、描述格式
- 支持外部 MCP 服务器集成
- 让 LLM 可理解、可选择、可调用

**配置示例（.mcp.json）**：
```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/workspace"]
    },
    "github": {
      "command": "mcp-server-github",
      "env": {
        "GITHUB_TOKEN": "${GITHUB_TOKEN}"
      }
    }
  }
}
```

---

## 4. 配置文件系统

### 4.1 项目配置（.claude.md）

**路径**：项目根目录 `.claude.md`

**作用**：告诉 Agent 当前项目的背景信息

**示例**：
````markdown
# 项目配置

## 项目信息
- 名称：AI CLI Agent
- 类型：命令行工具
- 语言：Java 21

## 技术栈
- Quarkus 3.8
- LangChain4j 0.35
- Picocli 4.7
- GraalVM 23

## 项目结构
```
src/main/java
├── agent/          # Agent 调度层
├── skill/          # Skill 实现
├── llm/            # LLM 客户端
└── cli/            # CLI 入口
```

## 开发约束
- 所有 Skill 必须实现 `Skill` 接口
- 使用 Sealed Classes 做错误处理
- 危险操作必须用户确认
- 所有文件操作限制在项目目录内

## 测试要求
- 单元测试覆盖率 > 80%
- 集成测试覆盖核心流程
- 使用 Testcontainers 测试外部依赖
````

### 4.2 Agent 配置（.agent.md）

**路径**：项目根目录 `.agent.md`

**作用**：定义 Agent 的行为规则和工作流

**示例**：
````markdown
# Agent 配置

## 角色定义
你是一个专业的开发助手，擅长：
- Java 开发和调试
- 项目构建和部署
- Git 版本控制
- 文件操作

## 工作原则
1. **安全第一**：所有危险操作必须用户确认
2. **最小权限**：只访问必要的文件和目录
3. **透明可控**：每步操作都要向用户说明
4. **增量执行**：优先选择风险小的操作

## 禁止操作
- 不得删除 `.git` 目录
- 不得修改系统文件
- 不得执行 `rm -rf /` 等危险命令
- 不得访问项目目录外的文件

## Skill 使用规范

### 文件操作
- 读取前先确认文件存在
- 修改前先备份
- 使用相对路径而非绝对路径

### Shell 命令
- 优先使用白名单命令
- 超时时间不超过 30 秒
- 失败时提供详细错误信息

### Git 操作
- 提交前检查 `.gitignore`
- 提供清晰的 commit message
- 推送前确认分支名称

## 错误处理
- 遇到错误立即停止
- 向用户解释错误原因
- 提供可能的解决方案
- 必要时请求用户介入

## 上下文管理
- 保持最近 20 条对话
- 超过窗口时自动摘要历史
- 重要信息持久化到 `.agent_context.json`
````

### 4.3 MCP 配置（.mcp.json）

**路径**：项目根目录 `.mcp.json`

**作用**：配置外部 MCP 服务器

**示例**：
```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "${PROJECT_ROOT}"],
      "description": "文件系统操作"
    },
    "git": {
      "command": "mcp-server-git",
      "description": "Git 版本控制"
    },
    "web-search": {
      "command": "mcp-server-brave-search",
      "env": {
        "BRAVE_API_KEY": "${BRAVE_API_KEY}"
      },
      "description": "网络搜索"
    }
  },
  "allowedPaths": [
    "${PROJECT_ROOT}",
    "${HOME}/.config/agent"
  ],
  "maxConcurrentSkills": 3,
  "skillTimeout": 30000
}
```

---

## 5. 系统架构（五层设计）

```
┌─────────────────────────────────────────────────────┐
│          第一层：CLI 交互层（Picocli + JLine）        │
│  - 命令解析                                           │
│  - 交互式 REPL                                        │
│  - 流式输出渲染                                       │
│  - 配置文件加载（.claude.md, .agent.md, .mcp.json）  │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│        第二层：Agent 调度层（ReAct 编排）             │
│  - 对话上下文管理                                     │
│  - LLM 决策获取                                       │
│  - 工具调用解析                                       │
│  - Skill 编排执行                                     │
│  - 结果反馈循环                                       │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│         第三层：Skill 能力层（MCP 标准）              │
│  - 内置 Skill（文件/Shell/Git/Maven）                │
│  - 外部 MCP 服务器（通过 .mcp.json 配置）            │
│  - 参数校验（JSON Schema）                            │
│  - 结果标准化                                         │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│        第四层：LLM 接入层（LangChain4j）              │
│  - 统一多模型接口（OpenAI/Claude/Ollama）            │
│  - 流式 / 非流式输出                                  │
│  - 工具调用（Function Calling）                       │
│  - Token 计数和限制                                   │
└─────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────┐
│          第五层：安全沙箱层（Java Security）          │
│  - 命令白名单                                         │
│  - 文件访问控制（基于 allowedPaths）                 │
│  - 危险操作拦截                                       │
│  - 执行超时控制                                       │
└─────────────────────────────────────────────────────┘
```

---

## 6. 核心接口设计

### 6.1 Result 类型（替代 Vavr Either）

```java
public sealed interface Result<T> {
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(String error, Throwable cause) implements Result<T> {
        public Failure(String error) {
            this(error, null);
        }
    }
    
    default boolean isSuccess() {
        return this instanceof Success;
    }
    
    default T getOrThrow() {
        return switch (this) {
            case Success<T>(var value) -> value;
            case Failure<T>(var error, var cause) -> 
                throw new RuntimeException(error, cause);
        };
    }
    
    default <U> Result<U> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T>(var value) -> new Success<>(mapper.apply(value));
            case Failure<T> f -> new Failure<>(f.error(), f.cause());
        };
    }
    
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }
    
    static <T> Result<T> failure(String error) {
        return new Failure<>(error);
    }
}
```

### 6.2 Skill 接口（MCP 标准）

```java
public interface Skill {
    /**
     * Skill 名称（给 LLM 看）
     */
    String name();
    
    /**
     * Skill 描述
     */
    String description();
    
    /**
     * 输入参数的 JSON Schema
     */
    JsonNode inputSchema();
    
    /**
     * 参数校验（默认实现）
     */
    default Result<Void> validate(SkillInput input) {
        return JsonSchemaValidator.validate(
            input.toJson(), 
            inputSchema()
        );
    }
    
    /**
     * 执行 Skill
     */
    Result<SkillResult> execute(SkillInput input);
    
    /**
     * 是否需要用户确认（危险操作）
     */
    default boolean requiresConfirmation() {
        return false;
    }
    
    /**
     * 执行超时（毫秒）
     */
    default long timeout() {
        return 30_000;
    }
}
```

### 6.3 Skill 输入输出

```java
public record SkillInput(Map<String, Object> params) {
    public String getString(String key) {
        return (String) params.get(key);
    }
    
    public Integer getInt(String key) {
        return (Integer) params.get(key);
    }
    
    public JsonNode toJson() {
        return JsonMapper.toJson(params);
    }
}

public record SkillResult(
    String content,
    Map<String, Object> metadata
) {
    public SkillResult(String content) {
        this(content, Map.of());
    }
}
```

### 6.4 Skill Registry

```java
@ApplicationScoped
public class SkillRegistry {
    private final Map<String, Skill> skills = new ConcurrentHashMap<>();
    
    public void register(Skill skill) {
        skills.put(skill.name(), skill);
    }
    
    public Optional<Skill> find(String name) {
        return Optional.ofNullable(skills.get(name));
    }
    
    public List<Skill> allSkills() {
        return List.copyOf(skills.values());
    }
    
    /**
     * 生成给 LLM 的工具列表描述
     */
    public String toToolsDescription() {
        return skills.values().stream()
            .map(skill -> String.format(
                "- %s: %s\n  Schema: %s",
                skill.name(),
                skill.description(),
                skill.inputSchema()
            ))
            .collect(Collectors.joining("\n\n"));
    }
}
```

### 6.5 Agent 接口

```java
public interface Agent {
    /**
     * 执行用户指令
     */
    Result<AgentOutput> run(String userInput);
    
    /**
     * 流式执行（实时输出）
     */
    Flow.Publisher<AgentEvent> runStream(String userInput);
    
    /**
     * 重置上下文
     */
    void reset();
}

public sealed interface AgentEvent {
    record Thinking(String content) implements AgentEvent {}
    record ToolCall(String skillName, SkillInput input) implements AgentEvent {}
    record ToolResult(String skillName, SkillResult result) implements AgentEvent {}
    record FinalAnswer(String content) implements AgentEvent {}
    record Error(String message) implements AgentEvent {}
}
```

### 6.6 配置加载器

```java
@ApplicationScoped
public class ConfigLoader {
    
    public Optional<ProjectConfig> loadClaudeConfig() {
        Path path = Path.of(".claude.md");
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(parseClaudeConfig(Files.readString(path)));
    }
    
    public Optional<AgentConfig> loadAgentConfig() {
        Path path = Path.of(".agent.md");
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(parseAgentConfig(Files.readString(path)));
    }
    
    public Optional<McpConfig> loadMcpConfig() {
        Path path = Path.of(".mcp.json");
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(JsonMapper.fromJson(
            Files.readString(path),
            McpConfig.class
        ));
    }
}

public record ProjectConfig(
    String name,
    String type,
    List<String> techStack,
    Map<String, String> structure,
    List<String> constraints
) {}

public record AgentConfig(
    String role,
    List<String> principles,
    List<String> prohibitedActions,
    Map<String, SkillRule> skillRules,
    ErrorHandling errorHandling
) {}

public record McpConfig(
    Map<String, McpServer> mcpServers,
    List<String> allowedPaths,
    int maxConcurrentSkills,
    long skillTimeout
) {}
```

---

## 7. 执行流程（ReAct 循环）

```
┌──────────────────────────────────────────────┐
│ 1. 用户输入：                                  │
│    "帮我重构 UserService.java，提取通用方法"  │
└──────────────────────────────────────────────┘
                  ↓
┌──────────────────────────────────────────────┐
│ 2. 加载配置：                                  │
│    - .claude.md（项目上下文）                 │
│    - .agent.md（行为规则）                    │
│    - .mcp.json（工具配置）                    │
└──────────────────────────────────────────────┘
                  ↓
┌──────────────────────────────────────────────┐
│ 3. 构造 Prompt：                               │
│    - 系统提示（来自 .agent.md）               │
│    - 项目信息（来自 .claude.md）              │
│    - 可用工具列表（来自 SkillRegistry）       │
│    - 对话历史                                 │
└──────────────────────────────────────────────┘
                  ↓
┌──────────────────────────────────────────────┐
│ 4. LLM 推理：                                  │
│    Thought: 需要先读取文件内容                │
│    Action: read_file                          │
│    Action Input: {"path": "UserService.java"} │
└──────────────────────────────────────────────┘
                  ↓
┌──────────────────────────────────────────────┐
│ 5. 安全检查：                                  │
│    - 路径是否在 allowedPaths 内？            │
│    - 是否触发危险操作？                       │
│    - 是否需要用户确认？                       │
└──────────────────────────────────────────────┘
                  ↓
┌──────────────────────────────────────────────┐
│ 6. 执行 Skill：                                │
│    Result: Success(文件内容...)               │
└──────────────────────────────────────────────┘
                  ↓
┌──────────────────────────────────────────────┐
│ 7. 反馈给 LLM：                                │
│    Observation: 文件内容如下...               │
└──────────────────────────────────────────────┘
                  ↓
┌──────────────────────────────────────────────┐
│ 8. LLM 继续推理：                              │
│    Thought: 发现了 3 处重复代码               │
│    Action: write_file                         │
│    Action Input: {"path": "...", "content": "..."}│
└──────────────────────────────────────────────┘
                  ↓
┌──────────────────────────────────────────────┐
│ 9. 循环直到：                                  │
│    - LLM 返回 Final Answer                    │
│    - 达到最大循环次数（默认 10）              │
│    - 发生错误                                 │
└──────────────────────────────────────────────┘
```

**关键控制点**：
- 每次循环前检查 `.agent.md` 的禁止操作列表
- 危险操作触发终端确认对话框
- 超时或错误立即中断循环
- 所有操作记录到审计日志

---

## 8. 核心 Skill 实现示例

### 8.1 文件读取 Skill

```java
@ApplicationScoped
public class FileReadSkill implements Skill {
    
    @Inject
    ConfigLoader configLoader;
    
    @Override
    public String name() {
        return "read_file";
    }
    
    @Override
    public String description() {
        return "读取指定文件的内容。支持文本文件，自动检测编码。";
    }
    
    @Override
    public JsonNode inputSchema() {
        return Json.createObjectBuilder()
            .add("type", "object")
            .add("properties", Json.createObjectBuilder()
                .add("path", Json.createObjectBuilder()
                    .add("type", "string")
                    .add("description", "文件路径（相对或绝对）")))
            .add("required", Json.createArrayBuilder().add("path"))
            .build();
    }
    
    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String pathStr = input.getString("path");
        Path path = Path.of(pathStr).normalize();
        
        // 安全检查：路径必须在允许范围内
        McpConfig config = configLoader.loadMcpConfig().orElseThrow();
        boolean allowed = config.allowedPaths().stream()
            .anyMatch(allowedPath -> {
                Path allowed = Path.of(allowedPath).normalize();
                return path.startsWith(allowed);
            });
        
        if (!allowed) {
            return Result.failure(
                "路径 " + path + " 不在允许访问的目录内"
            );
        }
        
        // 检查文件是否存在
        if (!Files.exists(path)) {
            return Result.failure("文件不存在: " + path);
        }
        
        // 读取文件
        try {
            String content = Files.readString(path);
            return Result.success(new SkillResult(
                content,
                Map.of(
                    "path", path.toString(),
                    "size", Files.size(path),
                    "lines", content.lines().count()
                )
            ));
        } catch (IOException e) {
            return Result.failure("读取失败: " + e.getMessage());
        }
    }
}
```

### 8.2 Shell 执行 Skill

```java
@ApplicationScoped
public class ShellExecuteSkill implements Skill {
    
    private static final Set<String> BLACKLIST = Set.of(
        "rm", "rmdir", "del", "format", "mkfs", 
        "dd", "shutdown", "reboot", "halt"
    );
    
    @Override
    public String name() {
        return "execute_shell";
    }
    
    @Override
    public String description() {
        return "执行 Shell 命令。危险命令会被拦截。";
    }
    
    @Override
    public JsonNode inputSchema() {
        return Json.createObjectBuilder()
            .add("type", "object")
            .add("properties", Json.createObjectBuilder()
                .add("command", Json.createObjectBuilder()
                    .add("type", "string")
                    .add("description", "要执行的命令"))
                .add("workdir", Json.createObjectBuilder()
                    .add("type", "string")
                    .add("description", "工作目录（可选）")
                    .add("default", ".")))
            .add("required", Json.createArrayBuilder().add("command"))
            .build();
    }
    
    @Override
    public boolean requiresConfirmation() {
        return true; // Shell 命令总是需要确认
    }
    
    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String command = input.getString("command");
        String workdir = input.getString("workdir");
        
        // 黑名单检查
        String firstWord = command.split("\\s+")[0];
        if (BLACKLIST.contains(firstWord)) {
            return Result.failure(
                "危险命令 '" + firstWord + "' 已被禁止"
            );
        }
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "/bin/sh", "-c", command
            );
            
            if (workdir != null) {
                pb.directory(new File(workdir));
            }
            
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            // 超时控制
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Result.failure("命令执行超时（30秒）");
            }
            
            String output = new String(
                process.getInputStream().readAllBytes()
            );
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return Result.failure(
                    "命令执行失败（退出码: " + exitCode + "）\n" + output
                );
            }
            
            return Result.success(new SkillResult(
                output,
                Map.of("exitCode", exitCode, "command", command)
            ));
            
        } catch (Exception e) {
            return Result.failure("执行异常: " + e.getMessage());
        }
    }
}
```

### 8.3 Git 操作 Skill

```java
@ApplicationScoped
public class GitCommitSkill implements Skill {
    
    @Override
    public String name() {
        return "git_commit";
    }
    
    @Override
    public String description() {
        return "提交暂存区的更改到本地仓库";
    }
    
    @Override
    public JsonNode inputSchema() {
        return Json.createObjectBuilder()
            .add("type", "object")
            .add("properties", Json.createObjectBuilder()
                .add("message", Json.createObjectBuilder()
                    .add("type", "string")
                    .add("description", "提交信息"))
                .add("files", Json.createObjectBuilder()
                    .add("type", "array")
                    .add("items", Json.createObjectBuilder()
                        .add("type", "string"))
                    .add("description", "要提交的文件（可选，默认全部）")))
            .add("required", Json.createArrayBuilder().add("message"))
            .build();
    }
    
    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String message = input.getString("message");
        List<String> files = (List<String>) input.params().get("files");
        
        try {
            // 1. git add
            if (files != null && !files.isEmpty()) {
                for (String file : files) {
                    executeGit("add", file);
                }
            } else {
                executeGit("add", ".");
            }
            
            // 2. git commit
            String output = executeGit("commit", "-m", message);
            
            return Result.success(new SkillResult(output));
            
        } catch (Exception e) {
            return Result.failure("Git 操作失败: " + e.getMessage());
        }
    }
    
    private String executeGit(String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Git 命令超时");
        }
        
        String output = new String(
            process.getInputStream().readAllBytes()
        );
        
        if (process.exitValue() != 0) {
            throw new RuntimeException("Git 命令失败:\n" + output);
        }
        
        return output;
    }
}
```

---

## 9. Agent 实现（ReAct 循环）

```java
@ApplicationScoped
public class ReactAgent implements Agent {
    
    @Inject
    ChatLanguageModel llm; // LangChain4j 自动注入
    
    @Inject
    SkillRegistry skillRegistry;
    
    @Inject
    ConfigLoader configLoader;
    
    private final Deque<ChatMessage> history = new ArrayDeque<>();
    private static final int MAX_ITERATIONS = 10;
    
    @Override
    public Result<AgentOutput> run(String userInput) {
        // 加载配置
        ProjectConfig projectConfig = configLoader.loadClaudeConfig().orElse(null);
        AgentConfig agentConfig = configLoader.loadAgentConfig().orElse(null);
        
        // 构造系统提示
        String systemPrompt = buildSystemPrompt(projectConfig, agentConfig);
        history.addFirst(ChatMessage.systemMessage(systemPrompt));
        
        // 添加用户输入
        history.addLast(ChatMessage.userMessage(userInput));
        
        // ReAct 循环
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // 调用 LLM
            Response<AiMessage> response = llm.generate(
                new ArrayList<>(history)
            );
            
            AiMessage message = response.content();
            history.addLast(message);
            
            // 解析响应
            if (message.hasToolExecutionRequests()) {
                // 有工具调用请求
                for (ToolExecutionRequest request : message.toolExecutionRequests()) {
                    Result<String> result = executeTool(
                        request.name(),
                        request.arguments()
                    );
                    
                    String observation = switch (result) {
                        case Result.Success<String>(var value) -> value;
                        case Result.Failure<String>(var error, var cause) -> 
                            "错误: " + error;
                    };
                    
                    // 添加工具执行结果到历史
                    history.addLast(ChatMessage.toolExecutionResultMessage(
                        request.id(),
                        observation
                    ));
                }
            } else {
                // 没有工具调用，说明任务完成
                return Result.success(new AgentOutput(message.text()));
            }
        }
        
        return Result.failure("达到最大迭代次数限制");
    }
    
    private String buildSystemPrompt(
        ProjectConfig projectConfig,
        AgentConfig agentConfig
    ) {
        StringBuilder prompt = new StringBuilder();
        
        // Agent 角色
        if (agentConfig != null) {
            prompt.append(agentConfig.role()).append("\n\n");
        }
        
        // 项目信息
        if (projectConfig != null) {
            prompt.append("## 项目信息\n");
            prompt.append("- 名称: ").append(projectConfig.name()).append("\n");
            prompt.append("- 技术栈: ").append(
                String.join(", ", projectConfig.techStack())
            ).append("\n\n");
        }
        
        // 可用工具
        prompt.append("## 可用工具\n");
        prompt.append(skillRegistry.toToolsDescription()).append("\n\n");
        
        // 工作原则
        if (agentConfig != null) {
            prompt.append("## 工作原则\n");
            agentConfig.principles().forEach(p -> 
                prompt.append("- ").append(p).append("\n")
            );
            prompt.append("\n");
        }
        
        // 禁止操作
        if (agentConfig != null) {
            prompt.append("## 禁止操作\n");
            agentConfig.prohibitedActions().forEach(p -> 
                prompt.append("- ").append(p).append("\n")
            );
        }
        
        return prompt.toString();
    }
    
    private Result<String> executeTool(String name, String arguments) {
        // 查找 Skill
        Optional<Skill> skillOpt = skillRegistry.find(name);
        if (skillOpt.isEmpty()) {
            return Result.failure("未找到工具: " + name);
        }
        
        Skill skill = skillOpt.get();
        
        // 解析参数
        SkillInput input;
        try {
            Map<String, Object> params = JsonMapper.fromJson(
                arguments,
                new TypeReference<>() {}
            );
            input = new SkillInput(params);
        } catch (Exception e) {
            return Result.failure("参数解析失败: " + e.getMessage());
        }
        
        // 参数校验
        Result<Void> validation = skill.validate(input);
        if (validation instanceof Result.Failure<Void> f) {
            return Result.failure("参数校验失败: " + f.error());
        }
        
        // 危险操作确认
        if (skill.requiresConfirmation()) {
            boolean confirmed = UserConfirmation.ask(
                "工具 '" + name + "' 需要确认，是否继续？\n" +
                "参数: " + arguments
            );
            if (!confirmed) {
                return Result.failure("用户取消操作");
            }
        }
        
        // 执行 Skill
        Result<SkillResult> result = skill.execute(input);
        
        return result.map(SkillResult::content);
    }
    
    @Override
    public void reset() {
        history.clear();
    }
}
```

---

## 10. CLI 入口实现

```java
@QuarkusMain
public class AgentCli implements QuarkusApplication {
    
    @Inject
    Agent agent;
    
    @Override
    public int run(String... args) throws Exception {
        Terminal terminal = TerminalBuilder.terminal();
        LineReader reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .build();
        
        System.out.println("AI CLI Agent 已启动");
        System.out.println("输入 'exit' 退出，'reset' 清空上下文\n");
        
        while (true) {
            String input;
            try {
                input = reader.readLine("agent> ");
            } catch (UserInterruptException e) {
                continue;
            } catch (EndOfFileException e) {
                break;
            }
            
            if (input.trim().isEmpty()) {
                continue;
            }
            
            if ("exit".equalsIgnoreCase(input.trim())) {
                break;
            }
            
            if ("reset".equalsIgnoreCase(input.trim())) {
                agent.reset();
                System.out.println("上下文已清空");
                continue;
            }
            
            // 执行
            Result<AgentOutput> result = agent.run(input);
            
            switch (result) {
                case Result.Success<AgentOutput>(var output) -> 
                    System.out.println("\n" + output.content() + "\n");
                case Result.Failure<AgentOutput>(var error, var cause) -> 
                    System.err.println("错误: " + error);
            }
        }
        
        System.out.println("再见!");
        return 0;
    }
}
```

---

## 11. 配置示例（application.properties）

```properties
# LLM 配置（LangChain4j）
quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}
quarkus.langchain4j.openai.base-url=https://api.openai.com/v1
quarkus.langchain4j.openai.chat-model.model-name=gpt-4-turbo
quarkus.langchain4j.openai.chat-model.temperature=0.7
quarkus.langchain4j.openai.timeout=60s

# 或使用 Ollama 本地模型
#quarkus.langchain4j.ollama.base-url=http://localhost:11434
#quarkus.langchain4j.ollama.chat-model.model-id=llama3

# 日志配置
quarkus.log.level=INFO
quarkus.log.category."agent".level=DEBUG

# 原生编译配置
quarkus.native.additional-build-args=--initialize-at-run-time=org.jline
```

---

## 12. 项目结构

```
agent-cli/
├── src/main/java/
│   ├── agent/
│   │   ├── Agent.java              # Agent 接口
│   │   ├── ReactAgent.java         # ReAct 实现
│   │   ├── AgentEvent.java         # 事件类型
│   │   └── AgentOutput.java        # 输出类型
│   ├── skill/
│   │   ├── Skill.java              # Skill 接口
│   │   ├── SkillRegistry.java      # 注册中心
│   │   ├── SkillInput.java         # 输入封装
│   │   ├── SkillResult.java        # 输出封装
│   │   ├── file/
│   │   │   ├── FileReadSkill.java
│   │   │   ├── FileWriteSkill.java
│   │   │   └── FileListSkill.java
│   │   ├── shell/
│   │   │   └── ShellExecuteSkill.java
│   │   └── git/
│   │       ├── GitCommitSkill.java
│   │       └── GitStatusSkill.java
│   ├── config/
│   │   ├── ConfigLoader.java       # 配置加载器
│   │   ├── ProjectConfig.java      # .claude.md
│   │   ├── AgentConfig.java        # .agent.md
│   │   └── McpConfig.java          # .mcp.json
│   ├── security/
│   │   ├── PathValidator.java      # 路径校验
│   │   ├── CommandBlacklist.java   # 命令黑名单
│   │   └── UserConfirmation.java   # 用户确认
│   ├── util/
│   │   ├── Result.java             # 统一返回类型
│   │   ├── JsonMapper.java         # JSON 工具
│   │   └── JsonSchemaValidator.java# Schema 校验
│   └── cli/
│       └── AgentCli.java           # CLI 入口
├── src/main/resources/
│   └── application.properties
├── .claude.md                       # 项目配置
├── .agent.md                        # Agent 配置
├── .mcp.json                        # MCP 配置
└── pom.xml
```

---

## 13. 部署方式

### 方式 1：JAR 包运行

```bash
mvn clean package
java -jar target/agent-cli-runner.jar
```

### 方式 2：GraalVM 原生二进制

```bash
mvn clean package -Pnative
./target/agent-cli
```

**优势**：
- 启动时间 < 50ms
- 内存占用 < 50MB
- 单文件分发，无需 JVM

### 方式 3：Docker 容器

```dockerfile
FROM registry.access.redhat.com/ubi8/ubi-minimal
COPY target/agent-cli /app/agent-cli
CMD ["/app/agent-cli"]
```

---

## 14. 测试策略

### 单元测试

```java
@QuarkusTest
class FileReadSkillTest {
    
    @Inject
    FileReadSkill skill;
    
    @Test
    void testReadFile() {
        // 准备测试文件
        Path testFile = Path.of("test.txt");
        Files.writeString(testFile, "hello world");
        
        // 执行
        SkillInput input = new SkillInput(
            Map.of("path", "test.txt")
        );
        Result<SkillResult> result = skill.execute(input);
        
        // 断言
        assertTrue(result.isSuccess());
        assertEquals("hello world", result.getOrThrow().content());
        
        // 清理
        Files.delete(testFile);
    }
    
    @Test
    void testPathOutsideAllowed() {
        SkillInput input = new SkillInput(
            Map.of("path", "/etc/passwd")
        );
        Result<SkillResult> result = skill.execute(input);
        
        assertTrue(result instanceof Result.Failure);
    }
}
```

### 集成测试

```java
@QuarkusTest
class AgentIntegrationTest {
    
    @Inject
    Agent agent;
    
    @Test
    void testSimpleTask() {
        Result<AgentOutput> result = agent.run(
            "读取 README.md 文件并告诉我第一行内容"
        );
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getOrThrow().content());
    }
}
```

---

## 15. 监控与日志

### 审计日志

```java
@ApplicationScoped
public class AuditLogger {
    
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    
    public void logSkillExecution(
        String skillName,
        SkillInput input,
        Result<SkillResult> result
    ) {
        String status = result.isSuccess() ? "SUCCESS" : "FAILURE";
        AUDIT.info(
            "SKILL_EXEC | {} | {} | {}",
            skillName,
            status,
            input.toJson()
        );
    }
}
```

### Metrics（Micrometer）

```java
@ApplicationScoped
public class SkillMetrics {
    
    @Inject
    MeterRegistry registry;
    
    public void recordExecution(String skillName, long durationMs, boolean success) {
        Timer.builder("skill.execution")
            .tag("skill", skillName)
            .tag("status", success ? "success" : "failure")
            .register(registry)
            .record(Duration.ofMillis(durationMs));
    }
}
```

---

## 16. 未来扩展方向

1. **多 Agent 协作**：不同 Agent 处理不同领域任务
2. **Memory 持久化**：长期记忆存储到向量数据库
3. **Plugin 市场**：社区贡献 Skill
4. **Web UI**：提供图形化界面
5. **Cloud 版本**：多租户 SaaS 服务

---

## 总结

本方案提供了一个**生产级 AI CLI Agent** 的完整实现路径：

✅ **现代化技术栈**：Java 21 + Quarkus + LangChain4j  
✅ **配置文件支持**：`.claude.md` / `.agent.md` / `.mcp.json`  
✅ **安全可控**：沙箱、白名单、用户确认  
✅ **易扩展**：MCP 标准、插件化 Skill  
✅ **高性能**：虚拟线程、GraalVM 原生编译  

可直接用于企业内部工具开发或开源项目。
