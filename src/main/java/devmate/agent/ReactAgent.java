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
 * ReactAgent 实现
 * 
 * 基于 ReAct（Reasoning and Acting）模式的 Agent 实现
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

    @Override
    public Result<AgentOutput> run(String userInput) {
        // 初始化系统提示
        if (!initialized) {
            initializeSystemPrompt();
            initialized = true;
        }

        // 添加用户输入
        history.add(UserMessage.from(userInput));

        int steps = 0;
        int toolCalls = 0;

        try {
            // ReAct 循环
            for (int i = 0; i < maxIterations; i++) {
                steps++;
                Log.debugf("Agent iteration %d/%d", i + 1, maxIterations);

                // 调用 LLM
                Response<AiMessage> response = chatModel.generate(new ArrayList<>(history));
                AiMessage aiMessage = response.content();
                history.add(aiMessage);

                // 检查是否有工具调用
                if (aiMessage.hasToolExecutionRequests()) {
                    // 处理所有工具调用
                    for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                        toolCalls++;
                        Result<String> toolResult = executeTool(request.name(), request.arguments());
                        
                        String observation = toolResult.isSuccess() 
                            ? toolResult.getOrThrow() 
                            : "错误: " + ((Result.Failure<?>) toolResult).error();
                        
                        history.add(ToolExecutionResultMessage.from(request, observation));
                    }
                } else {
                    // 没有工具调用，任务完成
                    return Result.success(AgentOutput.success(aiMessage.text(), steps, toolCalls));
                }
            }

            // 达到最大迭代次数
            return Result.failure("达到最大迭代次数限制 (" + maxIterations + ")");
            
        } catch (Exception e) {
            Log.errorf(e, "Agent execution failed");
            return Result.failure("Agent 执行失败: " + e.getMessage());
        }
    }

    @Override
    public Flow.Publisher<AgentEvent> runStream(String userInput) {
        SubmissionPublisher<AgentEvent> publisher = new SubmissionPublisher<>();

        // 异步执行
        new Thread(() -> {
            try {
                // 初始化系统提示
                if (!initialized) {
                    initializeSystemPrompt();
                    initialized = true;
                }

                history.add(UserMessage.from(userInput));

                for (int i = 0; i < maxIterations; i++) {
                    Response<AiMessage> response = chatModel.generate(new ArrayList<>(history));
                    AiMessage aiMessage = response.content();
                    history.add(aiMessage);

                    // 发布思考事件
                    if (aiMessage.text() != null && !aiMessage.text().isBlank()) {
                        publisher.submit(new AgentEvent.Thinking(aiMessage.text()));
                    }

                    if (aiMessage.hasToolExecutionRequests()) {
                        for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
                            // 发布工具调用事件
                            @SuppressWarnings("unchecked")
                            Map<String, Object> params = JsonMapper.fromJson(request.arguments(), Map.class);
                            publisher.submit(new AgentEvent.ToolCall(request.name(), new SkillInput(params)));

                            // 执行工具
                            Result<String> toolResult = executeTool(request.name(), request.arguments());
                            
                            // 发布工具结果事件
                            if (toolResult.isSuccess()) {
                                publisher.submit(new AgentEvent.ToolResult(
                                    request.name(), 
                                    new SkillResult(toolResult.getOrThrow())
                                ));
                            }
                            
                            String observation = toolResult.isSuccess() 
                                ? toolResult.getOrThrow() 
                                : "错误: " + ((Result.Failure<?>) toolResult).error();
                            
                            history.add(ToolExecutionResultMessage.from(request, observation));
                        }
                    } else {
                        // 完成
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
     * 初始化系统提示
     */
    private void initializeSystemPrompt() {
        String systemPrompt = buildSystemPrompt();
        history.clear();
        history.add(SystemMessage.from(systemPrompt));
    }

    /**
     * 构建系统提示
     */
    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();

        // 加载配置
        AgentConfig agentConfig = configLoader.loadAgentConfig().orElse(AgentConfig.defaultConfig());
        ProjectConfig projectConfig = configLoader.loadClaudeConfig().orElse(ProjectConfig.defaultConfig());

        // Agent 角色
        prompt.append("# 角色定义\n\n");
        prompt.append(agentConfig.role()).append("\n\n");

        // 项目信息
        prompt.append("# 项目信息\n\n");
        prompt.append("- 名称: ").append(projectConfig.name()).append("\n");
        prompt.append("- 类型: ").append(projectConfig.type()).append("\n");
        if (!projectConfig.techStack().isEmpty()) {
            prompt.append("- 技术栈: ").append(String.join(", ", projectConfig.techStack())).append("\n");
        }
        prompt.append("\n");

        // 可用工具
        prompt.append("# 可用工具\n\n");
        prompt.append(skillRegistry.toToolsDescription()).append("\n");

        // 工作原则
        if (!agentConfig.principles().isEmpty()) {
            prompt.append("# 工作原则\n\n");
            for (String principle : agentConfig.principles()) {
                prompt.append("- ").append(principle).append("\n");
            }
            prompt.append("\n");
        }

        // 禁止操作
        if (!agentConfig.prohibitedActions().isEmpty()) {
            prompt.append("# 禁止操作\n\n");
            for (String action : agentConfig.prohibitedActions()) {
                prompt.append("- ").append(action).append("\n");
            }
            prompt.append("\n");
        }

        // ReAct 指令
        prompt.append("# 执行模式\n\n");
        prompt.append("你是一个基于 ReAct (Reasoning and Acting) 模式的 AI Agent。\n");
        prompt.append("在每一步，你需要：\n");
        prompt.append("1. 思考当前需要做什么\n");
        prompt.append("2. 如果需要使用工具，调用相应的工具\n");
        prompt.append("3. 根据工具返回的结果继续思考\n");
        prompt.append("4. 当任务完成时，给出最终答案\n\n");
        prompt.append("请始终保持专业、安全、透明的态度。\n");

        return prompt.toString();
    }

    /**
     * 执行工具
     */
    private Result<String> executeTool(String name, String arguments) {
        Log.infof("Executing tool: %s with arguments: %s", name, arguments);

        // 查找 Skill
        return skillRegistry.find(name)
            .map(skill -> {
                // 解析参数
                SkillInput input;
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = JsonMapper.fromJson(arguments, Map.class);
                    input = new SkillInput(params);
                } catch (Exception e) {
                    return Result.<String>failure("参数解析失败: " + e.getMessage());
                }

                // 参数校验
                Result<Void> validation = skill.validate(input);
                if (validation.isFailure()) {
                    return Result.<String>failure("参数校验失败: " + ((Result.Failure<?>) validation).error());
                }

                // 危险操作确认
                if (skill.requiresConfirmation()) {
                    boolean confirmed = userConfirmation.ask(
                        "工具 '" + name + "' 需要确认，是否继续？\n参数: " + arguments
                    );
                    if (!confirmed) {
                        return Result.<String>failure("用户取消操作");
                    }
                }

                // 执行 Skill
                Result<SkillResult> result = skill.execute(input);
                
                return result.map(SkillResult::content);
            })
            .orElse(Result.failure("未找到工具: " + name));
    }
}
