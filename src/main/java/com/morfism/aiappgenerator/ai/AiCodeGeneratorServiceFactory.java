package com.morfism.aiappgenerator.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.morfism.aiappgenerator.service.ChatHistoryService;
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
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * AI service generate factory - supports multiple AI providers
 * 仿照简洁模式，支持多个AI模型
 */
@Slf4j
@Component
public class AiCodeGeneratorServiceFactory {

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    // Provider selection
    @Value("${ai.provider:openai}")
    private String provider;

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
    @Autowired
    private ChatHistoryService chatHistoryService;

    /**
     * 默认提供一个 Bean
     */
    @Bean
    public AiCodeGeneratorService aiCodeGeneratorService() {
        return getAiCodeGeneratorService(0L);
    }

    /**
     * 根据 appId 获取服务 (使用缓存)
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 创建 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        log.info("Creating AI service for provider: {}, appId: {}", provider, appId);
        
        return switch (provider.toLowerCase()) {
            case "openai" -> createOpenAiService(appId);
            case "claude" -> createClaudeService(appId);
            case "gemini" -> createGeminiService(appId);
            case "deepseek" -> createDeepSeekService(appId);
            default -> {
                log.warn("Unknown AI provider: {}, falling back to OpenAI", provider);
                yield createOpenAiService(appId);
            }
        };
    }

    /**
     * 创建 OpenAI 服务
     */
    private AiCodeGeneratorService createOpenAiService(long appId) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(openaiApiKey)
                .baseUrl(openaiBaseUrl)
                .modelName(openaiModelName)
                .maxCompletionTokens(openaiMaxCompletionTokens)
                .logRequests(openaiLogRequests)
                .logResponses(openaiLogResponses)
                .build();
        
        OpenAiStreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .apiKey(openaiApiKey)
                .baseUrl(openaiBaseUrl)
                .modelName(openaiModelName)
                .maxCompletionTokens(openaiMaxCompletionTokens)               
                .logRequests(openaiLogRequests)
                .logResponses(openaiLogResponses)
                .build();
        
        return buildAiService(chatModel, streamingChatModel, appId);
    }

    /**
     * 创建 Claude 服务
     */
    private AiCodeGeneratorService createClaudeService(long appId) {
        AnthropicChatModel chatModel = AnthropicChatModel.builder()
                .apiKey(claudeApiKey)
                .modelName(claudeModelName)
                .maxTokens(claudeMaxTokens)
                .temperature(claudeTemperature)
                .logRequests(claudeLogRequests)
                .logResponses(claudeLogResponses)
                .build();
        
        AnthropicStreamingChatModel streamingChatModel = AnthropicStreamingChatModel.builder()
                .apiKey(claudeApiKey)
                .modelName(claudeModelName)
                .maxTokens(claudeMaxTokens)
                .temperature(claudeTemperature)
                .logRequests(claudeLogRequests)
                .logResponses(claudeLogResponses)
                .build();
        
        return buildAiService(chatModel, streamingChatModel, appId);
    }

    /**
     * 创建 Gemini 服务
     */
    private AiCodeGeneratorService createGeminiService(long appId) {
        GoogleAiGeminiChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModelName)
                .maxOutputTokens(geminiMaxTokens)
                .temperature(geminiTemperature)
                .build();
        
        GoogleAiGeminiStreamingChatModel streamingChatModel = GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModelName)
                .maxOutputTokens(geminiMaxTokens)
                .temperature(geminiTemperature)
                .build();
        
        return buildAiService(chatModel, streamingChatModel, appId);
    }

    /**
     * 创建 DeepSeek 服务 (使用 OpenAI API 格式)
     */
    private AiCodeGeneratorService createDeepSeekService(long appId) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .apiKey(deepseekApiKey)
                .baseUrl(deepseekBaseUrl)
                .modelName(deepseekModelName)
                .maxCompletionTokens(deepseekMaxCompletionTokens)
                .temperature(deepseekTemperature)
                .logRequests(deepseekLogRequests)
                .logResponses(deepseekLogResponses)
                .build();
        
        OpenAiStreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .apiKey(deepseekApiKey)
                .baseUrl(deepseekBaseUrl)
                .modelName(deepseekModelName)
                .maxCompletionTokens(deepseekMaxCompletionTokens)
                .temperature(deepseekTemperature)
                .logRequests(deepseekLogRequests)
                .logResponses(deepseekLogResponses)
                .build();
        
        return buildAiService(chatModel, streamingChatModel, appId);
    }

    /**
     * 构建 AI 服务的通用方法
     */
    private AiCodeGeneratorService buildAiService(ChatModel chatModel, StreamingChatModel streamingChatModel, long appId) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();

        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
                
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * AI service instance cache
     * Cache strategy:
     * - Maximum cache size: 1000 instances
     * - Expire after write: 30 minutes
     * - Expire after access: 10 minutes
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI service instance removed, appId: {}, reason: {}", key, cause);
            })
            .build();




}