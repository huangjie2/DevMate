package devmate.agent;

import devmate.util.Result;

import java.util.concurrent.Flow;

/**
 * Agent Interface
 * 
 * Lightweight task orchestrator based on ReAct pattern
 */
public interface Agent {

    /**
     * Execute user instruction
     * 
     * @param userInput User input
     * @return Execution result
     */
    Result<AgentOutput> run(String userInput);

    /**
     * Stream execution (real-time output)
     * 
     * @param userInput User input
     * @return Event stream
     */
    Flow.Publisher<AgentEvent> runStream(String userInput);

    /**
     * Reset context
     */
    void reset();

    /**
     * Get current conversation history size
     */
    int getHistorySize();

    /**
     * Set maximum iterations
     */
    void setMaxIterations(int maxIterations);

    /**
     * Get maximum iterations
     */
    int getMaxIterations();
}