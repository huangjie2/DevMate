package devmate.agent;

/**
 * Agent Output Result
 */
public record AgentOutput(
    /**
     * Output content
     */
    String content,

    /**
     * Number of executed steps
     */
    int steps,

    /**
     * Number of tool calls
     */
    int toolCalls,

    /**
     * Whether successful
     */
    boolean success,

    /**
     * Error message (if failed)
     */
    String error
) {

    /**
     * Create success output
     */
    public static AgentOutput success(String content, int steps, int toolCalls) {
        return new AgentOutput(content, steps, toolCalls, true, null);
    }

    /**
     * Create failure output
     */
    public static AgentOutput failure(String error, int steps, int toolCalls) {
        return new AgentOutput(null, steps, toolCalls, false, error);
    }

    /**
     * Create simple success output
     */
    public static AgentOutput of(String content) {
        return success(content, 1, 0);
    }

    @Override
    public String toString() {
        if (success) {
            return content != null ? content : "";
        }
        return "Error: " + error;
    }
}