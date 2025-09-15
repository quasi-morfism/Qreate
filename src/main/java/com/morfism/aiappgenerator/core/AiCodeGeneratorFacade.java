package com.morfism.aiappgenerator.core;

import cn.hutool.core.util.StrUtil;
import com.morfism.aiappgenerator.ai.AiCodeGeneratorService;
//import com.morfism.aiappgenerator.ai.model.HtmlCodeResult;
//import com.morfism.aiappgenerator.ai.model.MultiFileCodeResult;

import com.morfism.aiappgenerator.ai.AiCodeGeneratorServiceFactory;
import com.morfism.aiappgenerator.core.parser.CodeParserExecutor;
import com.morfism.aiappgenerator.core.saver.CodeFileSaverExecutor;
import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.exception.ErrorCode;
import com.morfism.aiappgenerator.model.enums.CodeGenTypeEnum;
import com.morfism.aiappgenerator.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI代码生成门面类，整合代码生成和文件保存功能
 * AI code generation facade class, combining generation and saving functionality
 * 
 * 核心功能：
 * 1. 统一代码生成入口，支持同步和异步模式
 * 2. 根据生成类型自动选择合适的AI服务
 * 3. 集成代码解析和文件保存流程
 * 4. 提供响应式流式代码生成能力
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;
    
    @Resource
    private ChatHistoryService chatHistoryService;

//    /**
//     * 统一代码生成入口：根据类型生成并保存代码（同步模式）
//     * Unified entry point: generate and save code based on type
//     *
//     * @param userMessage     用户提示词/user prompt
//     * @param codeGenTypeEnum 代码生成类型/generation type
//     * @param appId           应用ID/application ID
//     * @return 保存的目录/saved directory
//     * @throws BusinessException 当生成类型为null或不支持时抛出异常
//     */
//    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
//        if (codeGenTypeEnum == null) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Generation type is null");
//        }
//        return switch (codeGenTypeEnum) {
//            case HTML -> {
//                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
//                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
//            }
//            case MULTI_FILE -> {
//                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
//                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
//            }
//            default -> {
//                String errorMessage = "Unsupported generation type: " + codeGenTypeEnum.getValue();
//                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
//            }
//        };
//    }
//
//
//
//    /**
//     * 处理代码流并在完成时保存到文件
//     * Process code stream and save to files when complete
//     *
//     * @param codestream 代码内容的响应式流/the reactive stream of code content
//     * @param codeType   代码生成类型/the type of code generation
//     * @param appId      应用ID/application ID
//     * @return 用于进一步处理的原始流/the original stream for further processing
//     */
//    private Flux<String> CodeStream(Flux<String> codestream, CodeGenTypeEnum codeType, Long appId) {
//        // Collect code chunks during streaming and save when complete
//        StringBuilder codeBuilder = new StringBuilder();
//        return codestream
//                .doOnNext(chunk -> {
//                    // Collect code chunks in real-time
//                    codeBuilder.append(chunk);
//                })
//                .doOnComplete(() -> {
//                    // Save code after streaming is complete
//                    try {
//                        String completeCode = codeBuilder.toString();
//                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeType);
//                        // Save parsed code to files
//                        File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeType, appId);
//                        log.info("Code saved successfully to: " + savedDir.getAbsolutePath());
//                    } catch (Exception e) {
//                        log.error("Failed to save code: {}", e.getMessage());
//                    }
//                });
//    }

    /**
     * 统一流式代码生成入口：根据类型生成并保存代码（异步流式模式）
     * Unified streaming entry point: generate and save code based on type
     * Provides reactive streaming of code generation with automatic file saving and chat history saving
     *
     * @param userMessage     用户代码生成提示词/user prompt for code generation
     * @param codeGenTypeEnum 要生成的代码类型(HTML或MULTI_FILE)/type of code to generate (HTML or MULTI_FILE)
     * @param appId           应用ID/application ID
     * @param userId          用户ID/user ID for chat history saving
     * @return 生成的代码块响应式流/reactive stream of generated code chunks
     * @throws BusinessException 当codeGenTypeEnum为null或不支持时抛出异常/if codeGenTypeEnum is null or unsupported
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId, Long userId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Code generation type is null");
        }
        if (StrUtil.isBlank(userMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "User message cannot be blank");
        }
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Application ID cannot be null or invalid");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "User ID cannot be null or invalid");
        }

        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        
        // 获取原始AI流
        Flux<String> originalStream = switch (codeGenTypeEnum) {
            case HTML -> aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
            case MULTI_FILE -> aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
            default -> {
                String errorMessage = "Unsupported code generation type: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
        
        // 转换为热流，支持多个订阅者
        Flux<String> sharedStream = originalStream.share();
        
        // 后台独立订阅：确保完整处理和存储
        StringBuilder codeBuilder = new StringBuilder();
        sharedStream
                .doOnNext(chunk -> codeBuilder.append(chunk))
                .doOnComplete(() -> {
                    // 后台保存逻辑，不依赖前端连接
                    String completeCode = codeBuilder.toString();
                    
                    // 1. 保存代码文件
                    try {
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenTypeEnum);
                        File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenTypeEnum, appId);
                        log.info("Background code saved successfully to: " + savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("Failed to save code in background: {}", e.getMessage());
                    }
                    
                    // 2. 保存聊天历史
                    try {
                        if (StrUtil.isNotBlank(completeCode)) {
                            chatHistoryService.saveAiMessage(appId, completeCode, userId);
                            log.info("Background chat history saved successfully for appId: {}", appId);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to save AI response to chat history in background: {}", e.getMessage());
                    }
                })
                .doOnError(throwable -> {
                    log.error("Background code processing failed: {}", throwable.getMessage());
                    // 保存错误消息到聊天历史
                    try {
                        chatHistoryService.saveErrorMessage(appId, 
                                "Code generation failed: " + throwable.getMessage(), 
                                userId);
                    } catch (Exception e) {
                        log.warn("Failed to save error message to chat history: {}", e.getMessage());
                    }
                })
                .subscribe(); // 后台独立订阅，不依赖前端连接
        
        // 返回共享流给前端
        return sharedStream;
    }


}
