package devmate.agent;

import devmate.config.AgentConfig;
import devmate.config.ConfigLoader;
import devmate.config.ProjectConfig;
import devmate.security.UserConfirmation;
import devmate.skill.Skill;
import devmate.skill.SkillInput;
import devmate.skill.SkillRegistry;
import devmate.skill.SkillResult;
import devmate.util.JsonMapper;
import devmate.util.Result;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * ReactAgent Implementation
 * 
 * Based on ReAct (Reasoning and Acting) pattern
 * Supports planning mode: analyze task, generate todo list, then execute step by step
 */
@ApplicationScoped
public class ReactAgent implements Agent {

    @Inject
    ChatLanguageModel chatModel;

    @Inject
    SkillRegistry skillRegistry;

    @Inject
    ConfigLoader configLoader;

    @Inject
    UserConfirmation userConfirmation;

    private final List<ChatMessage> history = new ArrayList<>();
    private int maxIterations = 10;
    private boolean initialized = false;
    
    // Current task plan
    private List<TaskPlan> currentPlan = new ArrayList<>();
    private int currentStep = 0;

    @Override
    public Result<AgentOutput> run(String userInput) {
        // Initialize system prompt
        if (!initialized) {
            initializeSystemPrompt();
            initialized = true;
        }

        // Add user input
        history.add(UserMessage.from(userInput));

        int steps = 0;
        int toolCalls = 0;

        try {
            // ReAct loop
            for (int i = 0; i < maxIterations; i++) {
                steps++;
                Log.debugf("Agent iteration %d/%d", i + 1, maxIterations);

                // Call LLM
                Response<AiMessage> response = chatModel.generate(new ArrayList<>(history));
                AiMessage aiMessage = response.content();
                history.add(aiMessage);

                // Check if there are tool calls
                if (aiMessage.hasToolExecutionRequests()) {
                    // Process all tool calls
                    for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                        toolCalls++;
                        Result<String> toolResult = executeTool(request.name(), request.arguments());
                        
                        String observation = toolResult.isSuccess() 
                            ? toolResult.getOrThrow() 
                            : "Error: " + ((Result.Failure<?>) toolResult).error();
                        
                        history.add(ToolExecutionResultMessage.from(request, observation));
                    }
                } else {
                    // No tool calls, task complete
                    return Result.success(AgentOutput.success(aiMessage.text(), steps, toolCalls));
                }
            }

            // Reached max iterations
            return Result.failure("Maximum iteration limit reached (" + maxIterations + ")");
            
        } catch (Exception e) {
            Log.errorf(e, "Agent execution failed");
            return Result.failure("Agent execution failed: " + e.getMessage());
        }
    }

    @Override
    public Flow.Publisher<AgentEvent> runStream(String userInput) {
        SubmissionPublisher<AgentEvent> publisher = new SubmissionPublisher<>();

        // Async execution
        new Thread(() -> {
            try {
                // Initialize system prompt
                if (!initialized) {
                    initializeSystemPrompt();
                    initialized = true;
                }

                history.add(UserMessage.from(userInput));

                for (int i = 0; i < maxIterations; i++) {
                    Response<AiMessage> response = chatModel.generate(new ArrayList<>(history));
                    AiMessage aiMessage = response.content();
                    history.add(aiMessage);

                    // Publish thinking event
                    if (aiMessage.text() != null && !aiMessage.text().isBlank()) {
                        publisher.submit(new AgentEvent.Thinking(aiMessage.text()));
                    }

                    if (aiMessage.hasToolExecutionRequests()) {
                        for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                            // Publish tool call event
                            @SuppressWarnings("unchecked")
                            Map<String, Object> params = JsonMapper.fromJson(request.arguments(), Map.class);
                            publisher.submit(new AgentEvent.ToolCall(request.name(), new SkillInput(params)));

                            // Execute tool
                            Result<String> toolResult = executeTool(request.name(), request.arguments());
                            
                            // Publish tool result event
                            if (toolResult.isSuccess()) {
                                publisher.submit(new AgentEvent.ToolResult(
                                    request.name(), 
                                    new SkillResult(toolResult.getOrThrow())
                                ));
                            }
                            
                            String observation = toolResult.isSuccess() 
                                ? toolResult.getOrThrow() 
                                : "Error: " + ((Result.Failure<?>) toolResult).error();
                            
                            history.add(ToolExecutionResultMessage.from(request, observation));
                        }
                    } else {
                        // Complete
                        publisher.submit(new AgentEvent.FinalAnswer(aiMessage.text()));
                        break;
                    }
                }

                publisher.close();
            } catch (Exception e) {
                publisher.submit(new AgentEvent.Error(e.getMessage(), e));
                publisher.close();
            }
        }).start();

        return publisher;
    }

    @Override
    public void reset() {
        history.clear();
        initialized = false;
        Log.info("Agent context has been reset");
    }

    @Override
    public int getHistorySize() {
        return history.size();
    }

    @Override
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = Math.max(1, maxIterations);
    }

    @Override
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * Initialize system prompt
     */
    private void initializeSystemPrompt() {
        String systemPrompt = buildSystemPrompt();
        history.clear();
        history.add(SystemMessage.from(systemPrompt));
    }

    /**
     * Build system prompt
     */
    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        // Load config
        AgentConfig agentConfig = configLoader.loadAgentConfig().orElse(AgentConfig.defaultConfig());
        ProjectConfig projectConfig = configLoader.loadClaudeConfig().orElse(ProjectConfig.defaultConfig());

        // Agent role
        prompt.append("# Role Definition\n\n");
        prompt.append(agentConfig.role()).append("\n\n");

        // Project info
        prompt.append("# Project Information\n\n");
        prompt.append("- Name: ").append(projectConfig.name()).append("\n");
        prompt.append("- Type: ").append(projectConfig.type()).append("\n");
        if (!projectConfig.techStack().isEmpty()) {
            prompt.append("- Tech Stack: ").append(String.join(", ", projectConfig.techStack())).append("\n");
        }
        prompt.append("\n");

        // Available tools
        prompt.append("# Available Tools\n\n");
        prompt.append(skillRegistry.toToolsDescription()).append("\n");

        // Working principles
        if (!agentConfig.principles().isEmpty()) {
            prompt.append("# Working Principles\n\n");
            for (String principle : agentConfig.principles()) {
                prompt.append("- ").append(principle).append("\n");
            }
            prompt.append("\n");
        }

        // Prohibited actions
        if (!agentConfig.prohibitedActions().isEmpty()) {
            prompt.append("# Prohibited Actions\n\n");
            for (String action : agentConfig.prohibitedActions()) {
                prompt.append("- ").append(action).append("\n");
            }
            prompt.append("\n");
        }

        // ReAct instructions
        prompt.append("# Execution Mode\n\n");
        prompt.append("You are an AI Agent based on ReAct (Reasoning and Acting) pattern.\n\n");
        
        // Planning mode
        prompt.append("## Task Planning\n\n");
        prompt.append("For complex, multi-step tasks, you must plan first, then execute:\n\n");
        prompt.append("**Step 1: Generate Todo List**\n");
        prompt.append("Before executing any operation, output the task plan:\n");
        prompt.append("```\n");
        prompt.append("📋 **Task Plan**\n");
        prompt.append("- [ ] 1. First step description\n");
        prompt.append("- [ ] 2. Second step description\n");
        prompt.append("...\n");
        prompt.append("```\n\n");
        prompt.append("**Step 2: Execute Step by Step**\n");
        prompt.append("Follow the plan, update status after each step:\n");
        prompt.append("```\n");
        prompt.append("- [x] 1. First step description ✓ Completed\n");
        prompt.append("- [ ] 2. Currently executing...\n");
        prompt.append("```\n\n");
        prompt.append("**Step 3: Summarize Results**\n");
        prompt.append("After all steps complete, provide the final answer.\n\n");
        
        prompt.append("## Single-Step Tasks\n");
        prompt.append("For simple, single-step tasks, execute directly without planning.\n\n");
        
        prompt.append("Always maintain a professional, safe, and transparent attitude. Confirm before executing any dangerous operations.");

        return prompt.toString();
    }

    /**
     * Execute tool
     */
    private Result<String> executeTool(String name, String arguments) {
        Log.infof("Executing tool: %s with arguments: %s", name, arguments);

        // Find Skill
        return skillRegistry.find(name)
            .map(skill -> {
                // Parse arguments
                SkillInput input;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = JsonMapper.fromJson(arguments, Map.class);
                    input = new SkillInput(params);
                } catch (Exception e) {
                    return Result.<String>failure("Failed to parse arguments: " + e.getMessage());
                }

                // Validate arguments
                Result<Void> validation = skill.validate(input);
                if (validation.isFailure()) {
                    return Result.<String>failure("Argument validation failed: " + ((Result.Failure<?>) validation).error());
                }

                // Confirm dangerous operations
                if (skill.requiresConfirmation()) {
                    boolean confirmed = userConfirmation.ask(
                        "Tool '" + name + "' requires confirmation. Continue?\nArguments: " + arguments
                    );
                    if (!confirmed) {
                        return Result.<String>failure("User cancelled operation");
                    }
                }

                // Execute Skill
                Result<SkillResult> result = skill.execute(input);
                
                return result.map(SkillResult::content);
            })
            .orElse(Result.failure("Tool not found: " + name));
    }
}