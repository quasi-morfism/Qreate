package com.morfism.aiappgenerator.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.morfism.aiappgenerator.ai.tools.FileWriteTool;
import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.exception.ErrorCode;
import com.morfism.aiappgenerator.model.enums.CodeGenTypeEnum;
import com.morfism.aiappgenerator.service.ChatHistoryService;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.service.AiServices;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * AI service generate factory - supports multiple AI providers
 * Follows a simplified pattern, supports multiple AI models
 */
@Slf4j
@Component
public class AiCodeGeneratorServiceFactory {

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    // Provider selection
    @Value("${ai.provider:deepseek}")
    private String defaultProvider;

    // OpenAI configuration
    @Value("${ai.openai.api-key:}")
    private String openaiApiKey;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${ai.openai.model-name:gpt-4o-mini}")
    private String openaiModelName;

    @Value("${ai.openai.max-completion-tokens:8192}")
    private Integer openaiMaxCompletionTokens;

    @Value("${ai.openai.temperature:1.0}")
    private Double openaiTemperature;

    @Value("${ai.openai.log-requests:true}")
    private Boolean openaiLogRequests;

    @Value("${ai.openai.log-responses:true}")
    private Boolean openaiLogResponses;

    // Claude configuration
    @Value("${ai.claude.api-key:}")
    private String claudeApiKey;

    @Value("${ai.claude.model-name:claude-3-5-sonnet-20241022}")
    private String claudeModelName;

    @Value("${ai.claude.max-tokens:8192}")
    private Integer claudeMaxTokens;

    @Value("${ai.claude.temperature:1.0}")
    private Double claudeTemperature;

    @Value("${ai.claude.log-requests:true}")
    private Boolean claudeLogRequests;

    @Value("${ai.claude.log-responses:true}")
    private Boolean claudeLogResponses;

    // Gemini configuration
    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.gemini.model-name:gemini-1.5-flash}")
    private String geminiModelName;

    @Value("${ai.gemini.max-tokens:8192}")
    private Integer geminiMaxTokens;

    @Value("${ai.gemini.temperature:1.0}")
    private Double geminiTemperature;

    @Value("${ai.gemini.log-requests:true}")
    private Boolean geminiLogRequests;

    @Value("${ai.gemini.log-responses:true}")
    private Boolean geminiLogResponses;

    // DeepSeek configuration (uses OpenAI API format)
    @Value("${ai.deepseek.api-key:}")
    private String deepseekApiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Value("${ai.deepseek.model-name:deepseek-chat}")
    private String deepseekModelName;

    @Value("${ai.deepseek.max-completion-tokens:8192}")
    private Integer deepseekMaxCompletionTokens;

    @Value("${ai.deepseek.temperature:1.0}")
    private Double deepseekTemperature;

    @Value("${ai.deepseek.log-requests:true}")
    private Boolean deepseekLogRequests;

    @Value("${ai.deepseek.log-responses:true}")
    private Boolean deepseekLogResponses;

    // DeepSeek Reasoning configuration (uses OpenAI API format)
    @Value("${ai.deepseek-reasoning.api-key:}")
    private String deepseekReasoningApiKey;

    @Value("${ai.deepseek-reasoning.base-url:https://api.deepseek.com/v1}")
    private String deepseekReasoningBaseUrl;

    @Value("${ai.deepseek-reasoning.model-name:deepseek-reasoner}")
    private String deepseekReasoningModelName;

    @Value("${ai.deepseek-reasoning.max-completion-tokens:32768}")
    private Integer deepseekReasoningMaxCompletionTokens;

    @Value("${ai.deepseek-reasoning.log-requests:true}")
    private Boolean deepseekReasoningLogRequests;

    @Value("${ai.deepseek-reasoning.log-responses:true}")
    private Boolean deepseekReasoningLogResponses;
    @Autowired
    private ChatHistoryService chatHistoryService;

    /**
     * Provide a default Bean
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }

    /**
     * Get service by appId (with cache) - this method is for backward compatibility
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * Get service by appId and code generation type (with cache)
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * Get service by appId, code generation type and provider
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType, String provider) {
        return createAiCodeGeneratorService(appId, codeGenType, provider);
    }

    /**
     * Build cache key
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

    /**
     * Create new AI service instance (without specifying provider, use default)
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        return createAiCodeGeneratorService(appId, codeGenType, null);
    }

    /**
     * Create new AI service instance
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType, String provider) {
        // Use the passed provider, if null then use default provider
        String actualProvider = StringUtils.hasText(provider) ? provider : defaultProvider;
        
        // Special logic: DeepSeek + VUE_PROJECT automatically switches to DeepSeek Reasoning
        if ("deepseek".equalsIgnoreCase(actualProvider) && codeGenType == CodeGenTypeEnum.VUE_PROJECT) {
            actualProvider = "deepseek-reasoning";
        }
        
        log.info("Creating AI service for provider: {}, codeGenType: {}, appId: {}", actualProvider, codeGenType, appId);
        
        // Set unified variables based on provider
        String currentApiKey;
        String currentBaseUrl;
        String currentModelName;
        Integer currentMaxTokens;
        Double currentTemperature = null;
        Boolean currentLogRequests;
        Boolean currentLogResponses;
        String providerType; // Used to determine which type of model to create
        
        switch (actualProvider.toLowerCase()) {
            case "openai" -> {
                currentApiKey = openaiApiKey;
                currentBaseUrl = openaiBaseUrl;
                currentModelName = openaiModelName;
                currentMaxTokens = openaiMaxCompletionTokens;
                currentTemperature = openaiTemperature;
                currentLogRequests = openaiLogRequests;
                currentLogResponses = openaiLogResponses;
                providerType = "openai";
            }
            case "claude" -> {
                currentApiKey = claudeApiKey;
                currentBaseUrl = null; // Claude doesn't need baseUrl
                currentModelName = claudeModelName;
                currentMaxTokens = claudeMaxTokens;
                currentTemperature = claudeTemperature;
                currentLogRequests = claudeLogRequests;
                currentLogResponses = claudeLogResponses;
                providerType = "claude";
            }
            case "gemini" -> {
                currentApiKey = geminiApiKey;
                currentBaseUrl = null; // Gemini doesn't need baseUrl
                currentModelName = geminiModelName;
                currentMaxTokens = geminiMaxTokens;
                currentTemperature = geminiTemperature;
                currentLogRequests = geminiLogRequests;
                currentLogResponses = geminiLogResponses;
                providerType = "gemini";
            }
            case "deepseek" -> {
                currentApiKey = deepseekApiKey;
                currentBaseUrl = deepseekBaseUrl;
                currentModelName = deepseekModelName;
                currentMaxTokens = deepseekMaxCompletionTokens;
                currentTemperature = deepseekTemperature;
                currentLogRequests = deepseekLogRequests;
                currentLogResponses = deepseekLogResponses;
                providerType = "openai"; // DeepSeek uses OpenAI API format
            }
            case "deepseek-reasoning" -> {
                currentApiKey = deepseekReasoningApiKey;
                currentBaseUrl = deepseekReasoningBaseUrl;
                currentModelName = deepseekReasoningModelName;
                currentMaxTokens = deepseekReasoningMaxCompletionTokens;
                currentTemperature = null; // Reasoning model doesn't support temperature
                currentLogRequests = deepseekReasoningLogRequests;
                currentLogResponses = deepseekReasoningLogResponses;
                providerType = "openai"; // DeepSeek Reasoning uses OpenAI API format
            }
            default -> {
                log.warn("Unknown AI provider: {}, falling back to OpenAI", actualProvider);
                currentApiKey = openaiApiKey;
                currentBaseUrl = openaiBaseUrl;
                currentModelName = openaiModelName;
                currentMaxTokens = openaiMaxCompletionTokens;
                currentTemperature = openaiTemperature;
                currentLogRequests = openaiLogRequests;
                currentLogResponses = openaiLogResponses;
                providerType = "openai";
            }
        }
        
        // Build independent chat memory based on appId
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        
        // Load chat history from database into memory
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
        
        // Create unified ChatModel and StreamingChatModel
        ChatModel chatModel = createUnifiedChatModel(providerType, currentApiKey, currentBaseUrl, 
                currentModelName, currentMaxTokens, currentTemperature, currentLogRequests, currentLogResponses);
        StreamingChatModel streamingChatModel = createUnifiedStreamingChatModel(providerType, currentApiKey, currentBaseUrl,
                currentModelName, currentMaxTokens, currentTemperature, currentLogRequests, currentLogResponses);
        
        // Choose different model configurations based on code generation type
        return switch (codeGenType) {
            // Vue project generation uses reasoning model and tools
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .streamingChatModel(streamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(new FileWriteTool())
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                        toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()
                    ))
                    .build();
            // HTML and multi-file generation use default model
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(streamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "Unsupported code generation type: " + codeGenType.getValue());
        };
    }


    /**
     * Unified ChatModel creation
     */
    private ChatModel createUnifiedChatModel(String providerType, String apiKey, String baseUrl, 
            String modelName, Integer maxTokens, Double temperature, Boolean logRequests, Boolean logResponses) {
        return switch (providerType.toLowerCase()) {
            case "openai" -> {
                var builder = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .maxCompletionTokens(maxTokens)
                        .logRequests(logRequests)
                        .logResponses(logResponses);
                if (baseUrl != null) builder.baseUrl(baseUrl);
                if (temperature != null) builder.temperature(temperature);
                yield builder.build();
            }
            case "claude" -> {
                var builder = AnthropicChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .maxTokens(maxTokens)
                        .logRequests(logRequests)
                        .logResponses(logResponses);
                if (temperature != null) builder.temperature(temperature);
                yield builder.build();
            }
            case "gemini" -> {
                var builder = GoogleAiGeminiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .maxOutputTokens(maxTokens);
                if (temperature != null) builder.temperature(temperature);
                yield builder.build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                    "Unsupported provider type: " + providerType);
        };
    }

    /**
     * Unified StreamingChatModel creation
     */
    private StreamingChatModel createUnifiedStreamingChatModel(String providerType, String apiKey, String baseUrl,
            String modelName, Integer maxTokens, Double temperature, Boolean logRequests, Boolean logResponses) {
        return switch (providerType.toLowerCase()) {
            case "openai" -> {
                var builder = OpenAiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .maxCompletionTokens(maxTokens)
                        .logRequests(logRequests)
                        .logResponses(logResponses);
                if (baseUrl != null) builder.baseUrl(baseUrl);
                if (temperature != null) builder.temperature(temperature);
                yield builder.build();
            }
            case "claude" -> {
                var builder = AnthropicStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .maxTokens(maxTokens)
                        .logRequests(logRequests)
                        .logResponses(logResponses);
                if (temperature != null) builder.temperature(temperature);
                yield builder.build();
            }
            case "gemini" -> {
                var builder = GoogleAiGeminiStreamingChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .maxOutputTokens(maxTokens);
                if (temperature != null) builder.temperature(temperature);
                yield builder.build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, 
                    "Unsupported provider type: " + providerType);
        };
    }

    /**
     * AI service instance cache
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI service instance removed from cache, cache key: {}, reason: {}", key, cause);
            })
            .build();




}