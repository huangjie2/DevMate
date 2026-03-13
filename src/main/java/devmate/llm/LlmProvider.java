package devmate.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.quarkus.logging.Log;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

/**
 * LLM Provider - 提供 ChatLanguageModel 的 CDI Bean
 */
@ApplicationScoped
public class LlmProvider {

    @ConfigProperty(name = "openai.api-key", defaultValue = "${OPENAI_API_KEY}")
    String apiKey;

    @ConfigProperty(name = "openai.base-url", defaultValue = "https://api.openai.com/v1")
    String baseUrl;

    @ConfigProperty(name = "openai.model-name", defaultValue = "gpt-4-turbo")
    String modelName;

    @ConfigProperty(name = "openai.temperature", defaultValue = "0.7")
    Double temperature;

    @ConfigProperty(name = "openai.timeout", defaultValue = "60")
    Integer timeoutSeconds;

    @Produces
    @ApplicationScoped
    @Startup
    public ChatLanguageModel createChatModel() {
        // 解析环境变量
        String resolvedApiKey = resolveEnvVar(apiKey);
        String resolvedBaseUrl = resolveEnvVar(baseUrl);
        
        if (resolvedApiKey == null || resolvedApiKey.isBlank() || resolvedApiKey.equals("${OPENAI_API_KEY}")) {
            Log.warn("No OpenAI API key configured. Set OPENAI_API_KEY environment variable.");
            // 返回一个空实现，允许应用启动
            return new MockChatLanguageModel();
        }

        Log.infof("Creating OpenAI ChatModel: baseUrl=%s, model=%s", resolvedBaseUrl, modelName);

        return OpenAiChatModel.builder()
            .apiKey(resolvedApiKey)
            .baseUrl(resolvedBaseUrl)
            .modelName(modelName)
            .temperature(temperature)
            .timeout(Duration.ofSeconds(timeoutSeconds))
            .logRequests(true)
            .logResponses(true)
            .build();
    }

    private String resolveEnvVar(String value) {
        if (value == null) return null;
        
        // 处理 ${ENV_VAR} 格式
        if (value.startsWith("${") && value.endsWith("}")) {
            String envVar = value.substring(2, value.length() - 1);
            return System.getenv(envVar);
        }
        return value;
    }
}
