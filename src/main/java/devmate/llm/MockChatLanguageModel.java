package devmate.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

/**
 * Mock ChatLanguageModel - 用于无 API Key 时的降级
 */
public class MockChatLanguageModel implements ChatLanguageModel {

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        String lastUserMessage = messages.stream()
            .filter(m -> m instanceof dev.langchain4j.data.message.UserMessage)
            .map(m -> ((dev.langchain4j.data.message.UserMessage) m).singleText())
            .reduce((first, second) -> second)
            .orElse("");
        
        String response = """
            ⚠️  未配置 OpenAI API Key，无法执行 AI 推理。
            
            请设置环境变量：
              export OPENAI_API_KEY=your-api-key
            
            或在 application.properties 中配置：
              openai.api-key=your-api-key
            
            您输入的内容: %s
            """.formatted(lastUserMessage);
        
        return Response.from(AiMessage.from(response));
    }
}
