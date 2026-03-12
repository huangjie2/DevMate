package devmate.agent;

import devmate.skill.SkillInput;
import devmate.skill.SkillResult;
import devmate.util.Result;

import java.util.concurrent.Flow;

/**
 * Agent 事件接口 - 用于流式输出的类型
 */
public sealed interface AgentEvent {

    /**
     * 思考事件
     */
    record Thinking(String content) implements AgentEvent {
        public Thinking {
            if (content == null) content = "";
        }
    }

    /**
     * 工具调用事件
     */
    record ToolCall(String skillName, SkillInput input) implements AgentEvent {
        public ToolCall {
            if (skillName == null || skillName.isBlank()) {
                throw new IllegalArgumentException("skillName cannot be null or blank");
            }
        }
    }

    /**
     * 工具执行结果事件
     */
    record ToolResult(String skillName, SkillResult result) implements AgentEvent {
        public ToolResult {
            if (skillName == null || skillName.isBlank()) {
                throw new IllegalArgumentException("skillName cannot be null or blank");
            }
        }
    }

    /**
     * 最终回答事件
     */
    record FinalAnswer(String content) implements AgentEvent {
        public FinalAnswer {
            if (content == null) content = "";
        }
    }

    /**
     * 错误事件
     */
    record Error(String message, Throwable cause) implements AgentEvent {
        public Error(String message) {
            this(message, null);
        }

        public Error {
            if (message == null || message.isBlank()) {
                message = "Unknown error";
            }
        }
    }
}
