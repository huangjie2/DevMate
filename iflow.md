# DevMate - AI-Powered Development Assistant

## Project Overview

DevMate is an intelligent development assistant CLI built with **Java 21**, **Quarkus**, and **LangChain4j**. It leverages the **ReAct (Reasoning and Acting)** pattern to provide AI-powered code development, project building, and debugging capabilities.

## Features

- **ReAct Agent Pattern**: Systematic reasoning and execution for complex tasks
- **Task Planning**: Automatically generates todo lists for multi-step operations
- **Rich TUI**: Colorful terminal interface with ANSI styling
- **MCP Standard**: Implements Model Context Protocol for skill definitions
- **File Reference**: Use `@filename` to include file contents in prompts
- **Command Completion**: Tab completion for commands and file paths
- **Security First**: Path validation, command blacklist, user confirmation for dangerous operations

## Architecture

```
src/main/java/devmate/
├── agent/           # Agent core implementation
│   ├── Agent.java           # Agent interface
│   ├── ReactAgent.java      # ReAct pattern implementation
│   ├── AgentEvent.java      # Streaming events
│   ├── AgentOutput.java     # Output result
│   └── TaskPlan.java        # Task planning
├── cli/             # CLI interface
│   ├── DevMateCli.java      # Main entry point
│   ├── CliStyle.java        # ANSI styling utilities
│   ├── CommandCompleter.java # Command completion
│   ├── AutoSuggestionProvider.java # Real-time suggestions
│   └── FileReferenceParser.java # @file reference parsing
├── config/          # Configuration management
│   ├── ConfigLoader.java    # Load .claude.md, .agent.md, .mcp.json
│   ├── AgentConfig.java     # Agent configuration
│   ├── ProjectConfig.java   # Project configuration
│   └── McpConfig.java       # MCP configuration
├── llm/             # LLM integration
│   ├── LlmProvider.java     # ChatLanguageModel CDI producer
│   └── MockChatLanguageModel.java # Fallback when no API key
├── security/        # Security components
│   ├── CommandBlacklist.java # Dangerous command blocking
│   ├── PathValidator.java   # Path access control
│   └── UserConfirmation.java # Interactive confirmation
├── skill/           # Skill implementations
│   ├── Skill.java           # Skill interface (MCP standard)
│   ├── SkillRegistry.java   # Skill registration
│   ├── SkillInput.java      # Skill input wrapper
│   ├── SkillResult.java     # Skill execution result
│   ├── file/                # File operations
│   ├── git/                 # Git operations
│   └── shell/               # Shell execution
└── util/            # Utilities
    ├── Result.java          # Sealed class result type
    ├── JsonMapper.java      # JSON utilities
    └── JsonSchemaValidator.java # Schema validation
```

## Built-in Skills

| Skill | Description | Category |
|-------|-------------|----------|
| `read_file` | Read file contents with optional offset/limit | file |
| `write_file` | Write content to file (creates directories) | file |
| `list_directory` | List directory contents with filtering | file |
| `execute_shell` | Execute shell commands (with blacklist) | shell |
| `git_status` | Get Git repository status | git |
| `git_commit` | Commit staged changes | git |

## Commands

| Command | Description |
|---------|-------------|
| `/help` | Show help information |
| `/exit` | Exit program |
| `/reset` | Clear conversation context |
| `/skills` | List available skills |
| `/config` | Show current configuration |
| `/clear` | Clear screen |

## File References

Use `@filename` in your prompts to include file contents:

```
Analyze @src/main/java/App.java
Compare @file1.java and @file2.java
@.  # Include project structure
```

## Configuration Files

### `.agent.md` - Agent Configuration

```markdown
## Role Definition
You are a professional development assistant...

## Working Principles
- Security first: All dangerous operations require user confirmation
- Minimum privilege: Only access necessary files and directories
- Transparent: Explain each step to the user

## Prohibited Actions
- Do not delete .git directory
- Do not modify system files
- Do not execute rm -rf /
```

### `.claude.md` - Project Configuration

```markdown
## Project Info
- Name: DevMate
- Type: Java CLI Application

## Tech Stack
- Java 21
- Quarkus
- LangChain4j
- JLine

## Development Constraints
- Follow clean code principles
- Write unit tests for new features
```

### `.mcp.json` - MCP Configuration

```json
{
  "allowedPaths": ["${PROJECT_ROOT}", "${HOME}/.config/devmate"],
  "maxConcurrentSkills": 3,
  "skillTimeout": 30000
}
```

## Running

```bash
# Build
mvn clean package

# Run
java -jar target/quarkus-app/quarkus-run.jar

# Or with native image (requires GraalVM)
mvn package -Dnative
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `OPENAI_API_KEY` | OpenAI API key |
| `OPENAI_BASE_URL` | API base URL (optional, for proxies) |

## Tech Stack

- **Java 21**: Sealed classes, pattern matching, records
- **Quarkus 3.8.4**: CDI, configuration, logging
- **LangChain4j 0.35.0**: LLM integration, tool calling
- **JLine 3.25.1**: Terminal interaction, completion
- **Jackson**: JSON processing
- **json-schema-validator**: Input validation

## License

MIT License
