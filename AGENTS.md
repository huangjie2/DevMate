# DevMate CLI - AI 命令行智能体

## 项目概述

DevMate 是一个基于 **Java 21 + Quarkus + LangChain4j** 构建的 AI 命令行智能体工具。

**核心特性**：
- **Skill 原子化**：每个功能封装为独立的 Skill，可测试、可复用
- **MCP 标准化**：遵循 Model Control Plane 规范，支持外部工具集成
- **ReAct 模式**：基于 Reasoning and Acting 模式的智能调度
- **安全沙箱**：路径校验、命令黑名单、用户确认机制

**对标产品**：Anthropic Claude Code、阿里 iFlow、GitHub Copilot Workspace

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 开发语言（Sealed Classes、Pattern Matching） |
| Quarkus | 3.8.4 | 应用框架（CLI 支持、原生编译） |
| LangChain4j | 0.35.0 | LLM 集成（支持 OpenAI/Claude/Ollama） |
| JLine | 3.25.1 | 终端交互（REPL、自动补全） |
| Jackson | - | JSON 处理 |
| JSON Schema Validator | 1.5.0 | 参数校验 |

## 项目结构

```
src/main/java/devmate/
├── agent/                    # Agent 调度层
│   ├── Agent.java            # Agent 接口定义
│   ├── ReactAgent.java       # ReAct 模式实现
│   ├── AgentEvent.java       # 流式事件类型
│   └── AgentOutput.java      # 输出结果封装
├── skill/                    # Skill 能力层
│   ├── Skill.java            # Skill 接口（MCP 标准）
│   ├── SkillRegistry.java    # Skill 注册中心
│   ├── SkillInput.java       # 输入参数封装
│   ├── SkillResult.java      # 输出结果封装
│   ├── file/                 # 文件操作 Skills
│   │   ├── FileReadSkill.java
│   │   ├── FileWriteSkill.java
│   │   └── FileListSkill.java
│   ├── shell/                # Shell 操作 Skills
│   │   └── ShellExecuteSkill.java
│   └── git/                  # Git 操作 Skills
│       ├── GitStatusSkill.java
│       └── GitCommitSkill.java
├── config/                   # 配置层
│   ├── ConfigLoader.java     # 配置加载器
│   ├── ProjectConfig.java    # .claude.md 配置
│   ├── AgentConfig.java      # .agent.md 配置
│   └── McpConfig.java        # .mcp.json 配置
├── security/                 # 安全层
│   ├── PathValidator.java    # 路径访问控制
│   ├── CommandBlacklist.java # 命令黑名单
│   └── UserConfirmation.java # 用户确认交互
├── util/                     # 工具类
│   ├── Result.java           # Sealed Result 类型
│   ├── JsonMapper.java       # JSON 工具
│   └── JsonSchemaValidator.java
└── cli/
    └── DevMateCli.java       # CLI 入口
```

## 构建与运行

### 环境要求
- JDK 21+
- Maven 3.8+
- 设置 `OPENAI_API_KEY` 环境变量（或配置其他 LLM）

### 开发模式
```bash
export OPENAI_API_KEY=your-api-key
mvn clean quarkus:dev
```

### 打包运行
```bash
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar
```

### 原生编译（需要 GraalVM）
```bash
mvn clean package -Pnative
./target/devmate-cli
```

### 运行测试
```bash
mvn test
```

## 配置文件

### `.claude.md` - 项目配置
定义项目背景、技术栈、开发约束：
```markdown
## 项目信息
- 名称: DevMate CLI
- 类型: 命令行工具

## 技术栈
- Quarkus 3.8
- LangChain4j 0.35

## 开发约束
- 所有 Skill 必须实现 `Skill` 接口
- 危险操作必须用户确认
```

### `.agent.md` - Agent 行为配置
定义 Agent 角色和工作原则：
```markdown
## 角色定义
你是一个专业的开发助手...

## 工作原则
1. 安全第一
2. 最小权限
3. 透明可控

## 禁止操作
- 不得删除 `.git` 目录
- 不得执行 `rm -rf /` 等危险命令
```

### `.mcp.json` - MCP 服务器配置
定义外部工具和访问权限：
```json
{
  "mcpServers": { ... },
  "allowedPaths": ["${PROJECT_ROOT}"],
  "maxConcurrentSkills": 3,
  "skillTimeout": 30000
}
```

### `application.properties` - Quarkus 配置
LLM 连接配置：
```properties
quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}
quarkus.langchain4j.openai.chat-model.model-name=gpt-4-turbo
```

## 核心概念

### Skill 接口
所有功能封装为 Skill，实现统一接口：
```java
public interface Skill {
    String name();           // Skill 名称
    String description();    // 功能描述
    JsonNode inputSchema();  // JSON Schema 参数定义
    Result<SkillResult> execute(SkillInput input);  // 执行逻辑
    boolean requiresConfirmation();  // 是否需要用户确认
}
```

### ReAct 模式
Agent 基于 ReAct（Reasoning and Acting）循环工作：
1. **Thought**: 分析用户意图
2. **Action**: 选择并执行 Skill
3. **Observation**: 收集执行结果
4. 循环直到完成任务或达到最大迭代次数

### Result 类型
使用 Java 21 Sealed Classes 实现类型安全的返回：
```java
public sealed interface Result<T> {
    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(String error, Throwable cause) implements Result<T> {}
}
```

## 内置 Skills

| Skill | 描述 | 需要确认 |
|-------|------|---------|
| `read_file` | 读取文件内容 | 否 |
| `write_file` | 写入文件 | 是 |
| `list_directory` | 列出目录内容 | 否 |
| `execute_shell` | 执行 Shell 命令 | 是 |
| `git_status` | 获取 Git 状态 | 否 |
| `git_commit` | Git 提交 | 是 |

## CLI 命令

| 命令 | 说明 |
|------|------|
| `/help` | 显示帮助 |
| `/exit` | 退出程序 |
| `/reset` | 清空对话上下文 |
| `/skills` | 列出可用技能 |
| `/config` | 显示当前配置 |
| `/clear` | 清屏 |

## 开发规范

### 添加新 Skill
1. 在对应包下创建类实现 `Skill` 接口
2. 添加 `@ApplicationScoped` 注解
3. 在 `DevMateCli` 中注册 Skill

### 代码风格
- 使用 Java 21 特性（Sealed Classes、Pattern Matching、Records）
- 所有 Skill 返回 `Result<SkillResult>`
- 危险操作必须调用 `requiresConfirmation()` 返回 true

### 安全原则
- 文件操作限制在 `allowedPaths` 范围内
- Shell 命令经过黑名单过滤
- 敏感操作需要用户确认

## 参考文档

- [design.md](design.md) - 完整技术方案
- [Quarkus 官方文档](https://quarkus.io/)
- [LangChain4j 文档](https://docs.langchain4j.dev/)
