package devmate.agent;

/**
 * 任务计划项
 */
public record TaskPlan(
    int index,          // 序号
    String task,        // 任务描述
    String status,      // 状态: pending, in_progress, completed, failed
    String tool,        // 使用的工具
    String result       // 结果
) {
    public static TaskPlan pending(int index, String task) {
        return new TaskPlan(index, task, "pending", null, null);
    }
    
    public TaskPlan inProgress() {
        return new TaskPlan(index, task, "in_progress", tool, result);
    }
    
    public TaskPlan completed(String tool, String result) {
        return new TaskPlan(index, task, "completed", tool, result);
    }
    
    public TaskPlan failed(String error) {
        return new TaskPlan(index, task, "failed", tool, error);
    }
    
    public String format() {
        String icon = switch (status) {
            case "pending" -> "⏳";
            case "in_progress" -> "🔄";
            case "completed" -> "✅";
            case "failed" -> "❌";
            default -> "❓";
        };
        return String.format("%s [%d] %s", icon, index, task);
    }
}
