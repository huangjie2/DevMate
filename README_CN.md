# DevMate CLI

<p align="center">
  <strong>AI 驱动的命令行智能体</strong>
</p>

<p align="center">
  <a href="#功能特性">功能特性</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#配置说明">配置说明</a> •
  <a href="#内置技能">内置技能</a> •
  <a href="#开发指南">开发指南</a>
</p>

<p align="center">
  <a href="README.md">English</a> | 中文
</p>

---

DevMate 是一个基于 **Java 21 + Quarkus + LangChain4j** 构建的 AI 命令行智能体。通过 ReAct 模式的智能调度，为开发工作流提供智能助手能力。

## 功能特性

- **Skill 原子化架构**：每个功能封装为独立的 Skill，可测试、可复用
- **MCP 标准化**：遵循 Model Control Plane 规范，支持外部工具集成
- **ReAct 模式**：基于 Reasoning and Acting 循环的智能任务执行
- **安全沙箱**：路径校验、命令黑名单、用户确认三重安全机制
- **多模型支持**：支持 OpenAI、Anthropic Claude、Ollama 等多种 LLM

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- OpenAI API Key（或其他 LLM 服务）

### 安装运行

```bash
# 克隆仓库
git clone https://github.com/your-org/devmate-cli.git
cd devmate-cli

# 设置 API Key
export OPENAI_API_KEY=your-api-key

# 开发模式运行
mvn clean quarkus:dev
```

### 构建部署

```bash
# 构建 JAR 包
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar

# 构建原生可执行文件（需要 GraalVM）
mvn clean package -Pnative
./target/devmate-cli
```

## 配置说明

### LLM 配置

编辑 `src/main/resources/application.properties`：

```properties
# OpenAI 配置
quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}
quarkus.langchain4j.openai.chat-model.model-name=gpt-4-turbo

# 或使用 Ollama 本地模型
#quarkus.langchain4j.ollama.base-url=http://localhost:11434
#quarkus.langchain4j.ollama.chat-model.model-id=llama3
```

### 项目配置（`.claude.md`）

定义项目上下文信息：

```markdown
## 项目信息
- 名称: 我的项目
- 类型: Web 应用

## 技术栈
- Java 21, Spring Boot 3.2

## 开发约束
- 文件操作限制在项目目录内
- 危险命令需要用户确认
```

### Agent 行为配置（`.agent.md`）

定义 Agent 的行为规则：

```markdown
## 角色定义
你是一个专业的开发助手。

## 工作原则
1. 安全第一：危险操作需要确认
2. 最小权限：只访问必要文件
3. 透明可控：说明每个操作

## 禁止操作
- 禁止删除 .git 目录
- 禁止修改系统文件
```

### MCP 配置（`.mcp.json`）

定义外部工具和访问权限：

```json
{
  "allowedPaths": ["${PROJECT_ROOT}"],
  "maxConcurrentSkills": 3,
  "skillTimeout": 30000
}
```

## 内置技能

| 技能 | 描述 | 需确认 |
|------|------|--------|
| `read_file` | 读取文件内容 | 否 |
| `write_file` | 写入文件 | 是 |
| `list_directory` | 列出目录内容 | 否 |
| `execute_shell` | 执行 Shell 命令 | 是 |
| `git_status` | 获取 Git 状态 | 否 |
| `git_commit` | Git 提交 | 是 |

## CLI 命令

| 命令 | 说明 |
|------|------|
| `/help` | 显示帮助信息 |
| `/exit` | 退出程序 |
| `/reset` | 清空对话上下文 |
| `/skills` | 列出可用技能 |
| `/config` | 显示当前配置 |
| `/clear` | 清屏 |

## 开发指南

### 项目结构

```
src/main/java/devmate/
├── agent/          # Agent 调度层（ReAct 实现）
├── skill/          # Skill 能力层
├── config/         # 配置加载器
├── security/       # 安全模块
├── util/           # 工具类
└── cli/            # CLI 入口
```

### 添加新技能

1. 创建实现 `Skill` 接口的类：

```java
@ApplicationScoped
public class MyCustomSkill implements Skill {
    
    @Override
    public String name() {
        return "my_custom_skill";
    }
    
    @Override
    public String description() {
        return "技能功能描述";
    }
    
    @Override
    public JsonNode inputSchema() {
        // 定义输入参数的 JSON Schema
    }
    
    @Override
    public Result<SkillResult> execute(SkillInput input) {
        // 实现技能逻辑
    }
    
    @Override
    public boolean requiresConfirmation() {
        return true; // 如果是危险操作
    }
}
```

2. 在 `DevMateCli.java` 中注册技能

### 运行测试

```bash
mvn test
```

## 架构设计

```
┌─────────────────────────────────────────┐
│           CLI 交互层 (JLine)             │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        Agent 调度层 (ReAct 循环)         │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        Skill 能力层 (MCP 标准)           │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│       LLM 接入层 (LangChain4j)           │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│     安全沙箱层 (路径/命令校验)           │
└─────────────────────────────────────────┘
```

### ReAct 执行流程

1. **Thought**：分析用户意图
2. **Action**：选择并执行 Skill
3. **Observation**：收集执行结果
4. 循环直到完成任务或达到最大迭代次数

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21+ | 开发语言（Sealed Classes、Pattern Matching） |
| Quarkus | 3.8.4 | 应用框架（CLI 支持、原生编译） |
| LangChain4j | 0.35.0 | LLM 集成 |
| JLine | 3.25.1 | 终端交互 |
| Jackson | - | JSON 处理 |

## 开发规范

### 代码风格

- 使用 Java 21 特性：Sealed Classes、Pattern Matching、Records
- 所有 Skill 返回 `Result<SkillResult>` 类型
- 危险操作必须设置 `requiresConfirmation()` 返回 true

### 安全原则

- 文件操作限制在 `allowedPaths` 配置的路径范围内
- Shell 命令经过黑名单过滤
- 敏感操作需要用户交互确认

## 许可证

MIT License

## 贡献指南

欢迎贡献代码！提交 PR 前请阅读贡献指南。

---

<p align="center">
  灵感来源：<a href="https://github.com/anthropics/claude-code">Claude Code</a>、<a href="https://github.com/features/copilot">GitHub Copilot</a>
</p>
