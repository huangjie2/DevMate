package devmate.agent;

import devmate.util.Result;

import java.util.concurrent.Flow;

/**
 * Agent 接口
 * 
 * 基于 ReAct 模式的轻量级任务编排器
 */
public interface Agent {

    /**
     * 执行用户指令
     * 
     * @param userInput 用户输入
     * @return 执行结果
     */
    Result<AgentOutput> run(String userInput);

    /**
     * 流式执行（实时输出）
     * 
     * @param userInput 用户输入
     * @return 事件流
     */
    Flow.Publisher<AgentEvent> runStream(String userInput);

    /**
     * 重置上下文
     */
    void reset();

    /**
     * 获取当前对话历史大小
     */
    int getHistorySize();

    /**
     * 设置最大迭代次数
     */
    void setMaxIterations(int maxIterations);

    /**
     * 获取最大迭代次数
     */
    int getMaxIterations();
}
