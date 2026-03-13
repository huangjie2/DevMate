package devmate.agent;

import devmate.skill.SkillInput;
import devmate.skill.SkillResult;
import devmate.util.Result;

import java.util.concurrent.Flow;

/**
 * Agent Event Interface - Types for streaming output
 */
public sealed interface AgentEvent {

    /**
     * Thinking event
     */
    record Thinking(String content) implements AgentEvent {
        public Thinking {
            if (content == null) content = "";
        }
    }

    /**
     * Tool call event
     */
    record ToolCall(String skillName, SkillInput input) implements AgentEvent {
        public ToolCall {
            if (skillName == null || skillName.isBlank()) {
                throw new IllegalArgumentException("skillName cannot be null or blank");
            }
        }
    }

    /**
     * Tool execution result event
     */
    record ToolResult(String skillName, SkillResult result) implements AgentEvent {
        public ToolResult {
            if (skillName == null || skillName.isBlank()) {
                throw new IllegalArgumentException("skillName cannot be null or blank");
            }
        }
    }

    /**
     * Final answer event
     */
    record FinalAnswer(String content) implements AgentEvent {
        public FinalAnswer {
            if (content == null) content = "";
        }
    }

    /**
     * Error event
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