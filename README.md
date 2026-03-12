# DevMate CLI

<p align="center">
  <strong>AI-Powered Command Line Agent</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#quick-start">Quick Start</a> •
  <a href="#configuration">Configuration</a> •
  <a href="#built-in-skills">Skills</a> •
  <a href="#development">Development</a>
</p>

---

DevMate is an AI-powered command line agent built with **Java 21 + Quarkus + LangChain4j**. It provides an intelligent assistant for development workflows through a ReAct-based agent architecture.

## Features

- **Skill-Based Architecture**: Each capability is encapsulated as an independent, testable, and reusable Skill
- **MCP Standard**: Follows Model Control Plane specification for tool integration
- **ReAct Pattern**: Implements Reasoning and Acting loop for intelligent task execution
- **Security Sandbox**: Path validation, command blacklist, and user confirmation mechanisms
- **Multi-LLM Support**: Works with OpenAI, Anthropic Claude, and Ollama

## Quick Start

### Prerequisites

- JDK 21+
- Maven 3.8+
- OpenAI API Key (or other LLM provider)

### Installation

```bash
# Clone the repository
git clone https://github.com/your-org/devmate-cli.git
cd devmate-cli

# Set API key
export OPENAI_API_KEY=your-api-key

# Run in development mode
mvn clean quarkus:dev
```

### Build

```bash
# Build JAR
mvn clean package
java -jar target/quarkus-app/quarkus-run.jar

# Build native executable (requires GraalVM)
mvn clean package -Pnative
./target/devmate-cli
```

## Configuration

### LLM Configuration

Edit `src/main/resources/application.properties`:

```properties
# OpenAI
quarkus.langchain4j.openai.api-key=${OPENAI_API_KEY}
quarkus.langchain4j.openai.chat-model.model-name=gpt-4-turbo

# Or Ollama (local)
#quarkus.langchain4j.ollama.base-url=http://localhost:11434
#quarkus.langchain4j.ollama.chat-model.model-id=llama3
```

### Project Configuration (`.claude.md`)

Defines project context for the agent:

```markdown
## Project Info
- Name: My Project
- Type: Web Application

## Tech Stack
- Java 21, Spring Boot 3.2

## Constraints
- All file operations within project directory
- Dangerous commands require confirmation
```

### Agent Behavior (`.agent.md`)

Defines agent rules and principles:

```markdown
## Role
You are a professional development assistant.

## Principles
1. Safety first: confirm dangerous operations
2. Minimal privilege: access only necessary files
3. Transparency: explain each action

## Prohibited Actions
- No deleting .git directory
- No system file modifications
```

### MCP Configuration (`.mcp.json`)

Defines external tools and permissions:

```json
{
  "allowedPaths": ["${PROJECT_ROOT}"],
  "maxConcurrentSkills": 3,
  "skillTimeout": 30000
}
```

## Built-in Skills

| Skill | Description | Confirmation |
|-------|-------------|--------------|
| `read_file` | Read file contents | No |
| `write_file` | Write to file | Yes |
| `list_directory` | List directory contents | No |
| `execute_shell` | Execute shell command | Yes |
| `git_status` | Get git repository status | No |
| `git_commit` | Create git commit | Yes |

## CLI Commands

| Command | Description |
|---------|-------------|
| `/help` | Show help information |
| `/exit` | Exit the program |
| `/reset` | Clear conversation context |
| `/skills` | List available skills |
| `/config` | Show current configuration |
| `/clear` | Clear screen |

## Development

### Project Structure

```
src/main/java/devmate/
├── agent/          # Agent orchestration (ReAct)
├── skill/          # Skill implementations
├── config/         # Configuration loaders
├── security/       # Security modules
├── util/           # Utilities
└── cli/            # CLI entry point
```

### Adding a New Skill

1. Create a class implementing `Skill` interface:

```java
@ApplicationScoped
public class MyCustomSkill implements Skill {
    
    @Override
    public String name() {
        return "my_custom_skill";
    }
    
    @Override
    public String description() {
        return "Description of what this skill does";
    }
    
    @Override
    public JsonNode inputSchema() {
        // Define JSON Schema for input parameters
    }
    
    @Override
    public Result<SkillResult> execute(SkillInput input) {
        // Implement the skill logic
    }
    
    @Override
    public boolean requiresConfirmation() {
        return true; // If this is a dangerous operation
    }
}
```

2. Register the skill in `DevMateCli.java`

### Running Tests

```bash
mvn test
```

## Architecture

```
┌─────────────────────────────────────────┐
│           CLI Layer (JLine)             │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        Agent Layer (ReAct Loop)         │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│        Skill Layer (MCP Standard)       │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│       LLM Layer (LangChain4j)           │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│     Security Layer (Sandbox)            │
└─────────────────────────────────────────┘
```

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21+ | Language (Sealed Classes, Pattern Matching) |
| Quarkus | 3.8.4 | Framework (CLI, Native compilation) |
| LangChain4j | 0.35.0 | LLM integration |
| JLine | 3.25.1 | Terminal interaction |
| Jackson | - | JSON processing |

## License

MIT License

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

---

<p align="center">
  Inspired by <a href="https://github.com/anthropics/claude-code">Claude Code</a>, <a href="https://github.com/features/copilot">GitHub Copilot</a>
</p>
