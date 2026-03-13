package devmate.llm;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

import java.util.List;

/**
 * Mock ChatLanguageModel - Fallback when no API Key is configured
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
            ⚠️  OpenAI API Key not configured. Cannot perform AI inference.
            
            Please set the environment variable:
              export OPENAI_API_KEY=your-api-key
            
            Or configure in application.properties:
              openai.api-key=your-api-key
            
            Your input: %s
            """.formatted(lastUserMessage);
        
        return Response.from(AiMessage.from(response));
    }
}