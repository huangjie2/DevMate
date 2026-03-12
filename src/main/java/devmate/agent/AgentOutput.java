package devmate.agent;

/**
 * Agent 输出结果
 */
public record AgentOutput(
    /**
     * 输出内容
     */
    String content,

    /**
     * 执行的步骤数
     */
    int steps,

    /**
     * 调用的工具数
     */
    int toolCalls,

    /**
     * 是否成功
     */
    boolean success,

    /**
     * 错误信息（如果失败）
     */
    String error
) {

    /**
     * 创建成功输出
     */
    public static AgentOutput success(String content, int steps, int toolCalls) {
        return new AgentOutput(content, steps, toolCalls, true, null);
    }

    /**
     * 创建失败输出
     */
    public static AgentOutput failure(String error, int steps, int toolCalls) {
        return new AgentOutput(null, steps, toolCalls, false, error);
    }

    /**
     * 创建简单成功输出
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
