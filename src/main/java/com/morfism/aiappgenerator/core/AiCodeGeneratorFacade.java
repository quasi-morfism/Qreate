package com.morfism.aiappgenerator.core;

import cn.hutool.core.util.StrUtil;
import com.morfism.aiappgenerator.ai.AiCodeGeneratorService;
//import com.morfism.aiappgenerator.ai.model.HtmlCodeResult;
//import com.morfism.aiappgenerator.ai.model.MultiFileCodeResult;

import com.morfism.aiappgenerator.ai.AiCodeGeneratorServiceFactory;
import com.morfism.aiappgenerator.constant.AppConstant;
import com.morfism.aiappgenerator.core.builder.VueProjectBuilder;
import com.morfism.aiappgenerator.core.parser.CodeParserExecutor;
import com.morfism.aiappgenerator.core.saver.CodeFileSaverExecutor;
import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.exception.ErrorCode;
import com.morfism.aiappgenerator.model.enums.CodeGenTypeEnum;
import com.morfism.aiappgenerator.service.ChatHistoryService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.TokenStream;
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

    @Resource
    private VueProjectBuilder vueProjectBuilder;

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
     * @param codeGenTypeEnum 要生成的代码类型(HTML、MULTI_FILE或VUE_PROJECT)/type of code to generate (HTML, MULTI_FILE or VUE_PROJECT)
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

        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenTypeEnum);
        
        // 获取原始AI流
        Flux<String> originalStream = switch (codeGenTypeEnum) {
            case HTML -> aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
            case MULTI_FILE -> aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
            case VUE_PROJECT -> aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
            default -> {
                String errorMessage = "Unsupported code generation type: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };

        // 根据代码生成类型选择不同的流处理逻辑
        Flux<String> processedStream = switch (codeGenTypeEnum) {
            case HTML, MULTI_FILE -> processTraditionalStream(originalStream, codeGenTypeEnum, appId, userId);
            case VUE_PROJECT -> processVueProjectStreamWithToolCallbacks(userMessage, appId, userId);
            default -> originalStream; // fallback
        };
        
        // 转换为热流，支持多个订阅者
        return processedStream.share();
    }

    /**
     * 处理传统格式(HTML/MULTI_FILE)的流式传输
     * Process traditional format (HTML/MULTI_FILE) streaming
     */
    private Flux<String> processTraditionalStream(Flux<String> originalStream, CodeGenTypeEnum codeGenTypeEnum, Long appId, Long userId) {
        Flux<String> sharedStream = originalStream.share();
        
        // 后台独立订阅：确保完整处理和存储
        StringBuilder codeBuilder = new StringBuilder();
        sharedStream
                .doOnNext(codeBuilder::append)
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
        
        return sharedStream;
    }



    /**
     * 为Vue项目实现带有工具回调的流式传输（使用TokenStream）
     * Vue project streaming with tool callbacks using TokenStream API
     * 
     * 特点：
     * 1. 前端断开后后台继续执行
     * 2. 内存（聊天历史）持久化保存
     * 3. 支持前端重连恢复
     */
    private Flux<String> processVueProjectStreamWithToolCallbacks(String userMessage, Long appId, Long userId) {
        log.info("🎯 Using TokenStream with onToolExecuted callback for appId: {}", appId);
        
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, CodeGenTypeEnum.VUE_PROJECT);
        TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectTokenStream(appId, userMessage);
        
        // 使用多播热流，支持多个订阅者共享同一个流
        return Flux.<String>create(sink -> {
            StringBuilder responseBuilder = new StringBuilder();
            
            tokenStream
                    .onPartialResponse(partialResponse -> {
                        if (partialResponse != null) {
                            responseBuilder.append(partialResponse);
                            // 使用tryEmitNext非阻塞发送，前端断开不影响后端执行
                            sink.next(partialResponse);
                        }
                    })
                    .onToolExecuted(toolExecution -> {
                        // 精确的工具执行回调 - 即使前端断开也会执行
                        ToolExecutionRequest request = toolExecution.request();
                        String toolName = request.name();
                        String result = toolExecution.result();
                        
                        log.info("🛠️ Tool executed: {} (id: {}) for appId: {}", toolName, request.id(), appId);
                        
                        if ("writeFile".equals(toolName)) {
                            try {
                                String arguments = request.arguments();
                                String fileName = extractFileNameFromArguments(arguments);
                                
                                if (result != null && !result.contains("error") && !result.contains("failed")) {
                                    log.info("✅ File write completed: {} for appId: {}", fileName, appId);
                                    String successMarker = "\n[FILE_WRITE_SUCCESS:" + fileName + "]";
                                    responseBuilder.append(successMarker);
                                    log.info("📝 Added FILE_WRITE_SUCCESS marker to responseBuilder: {}", successMarker);
                                    sink.next(successMarker);
                                } else {
                                    log.warn("❌ File write failed: {} for appId: {}", fileName, appId);
                                    String failedMarker = "\n[FILE_WRITE_FAILED:" + fileName + "]";
                                    responseBuilder.append(failedMarker);
                                    log.info("📝 Added FILE_WRITE_FAILED marker to responseBuilder: {}", failedMarker);
                                    sink.next(failedMarker);
                                }
                            } catch (Exception e) {
                                log.warn("❌ Error processing writeFile tool execution: {}", e.getMessage());
                                String errorMarker = "\n[FILE_WRITE_ERROR]";
                                responseBuilder.append(errorMarker);
                                sink.next(errorMarker);
                            }
                        }
                        
                        // 注释掉工具执行ID的显示，避免前端显示乱七八糟的ID
                        // sink.next("\n[TOOL_EXECUTED:" + toolName + ":" + request.id() + "]");
                    })
                    .onCompleteResponse(completeResponse -> {
                        log.info("🎉 Vue project generation completed for appId: {}", appId);
                        
                        // 先添加完成标记到responseBuilder
                        String completeMarker = "\n[GENERATION_COMPLETE]";
                        responseBuilder.append(completeMarker);
                        
                        // 后台保存聊天历史 - 不依赖前端连接
                        try {
                            String fullResponse = responseBuilder.toString();
                            if (StrUtil.isNotBlank(fullResponse)) {
                                log.info("💾 Saving chat history for appId: {}, content length: {}, contains FILE_WRITE_SUCCESS: {}", 
                                    appId, fullResponse.length(), fullResponse.contains("[FILE_WRITE_SUCCESS"));
                                chatHistoryService.saveAiMessage(appId, fullResponse, userId);
                                log.info("💾 Memory saved: Vue project response for appId: {}", appId);
                            } else {
                                log.warn("⚠️ Empty response content, not saving to chat history for appId: {}", appId);
                            }
                        } catch (Exception e) {
                            log.warn("Failed to save Vue project response to chat history: {}", e.getMessage());
                        }
                        
                        // 异步构建Vue项目
                        try {
                            String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId;
                            vueProjectBuilder.buildProjectAsync(projectPath);
                            log.info("🔨 Started async build for Vue project at: {}", projectPath);
                        } catch (Exception e) {
                            log.warn("Failed to start async build for Vue project: {}", e.getMessage());
                        }
                        
                        // 发送完成标记给前端
                        sink.next(completeMarker);
                        sink.complete();
                    })
                    .onError(throwable -> {
                        log.error("Vue project generation failed: {}", throwable.getMessage());
                        
                        // 错误也要保存到内存中
                        try {
                            chatHistoryService.saveErrorMessage(appId, 
                                    "Vue project generation failed: " + throwable.getMessage(), 
                                    userId);
                        } catch (Exception e) {
                            log.warn("Failed to save error message to chat history: {}", e.getMessage());
                        }
                        
                        sink.error(throwable);
                    })
                    .start(); // TokenStream独立运行，不受前端连接影响
        }).share(); // 转换为热流，支持多个订阅者
    }



    /**
     * 从工具参数中提取文件名
     * Extract file name from tool arguments
     *
     * @param arguments 工具调用参数JSON字符串/tool arguments JSON string
     * @return 文件名/file name
     */
    private String extractFileNameFromArguments(String arguments) {
        try {
            // 工具参数通常是JSON格式，尝试提取relativeFilePath、fileName或path字段
            if (arguments != null) {
                // 查找relativeFilePath字段（Vue项目工具使用的字段名）
                if (arguments.contains("\"relativeFilePath\"")) {
                    int start = arguments.indexOf("\"relativeFilePath\"") + 19; // "relativeFilePath": 的长度
                    int valueStart = arguments.indexOf("\"", start) + 1;
                    int valueEnd = arguments.indexOf("\"", valueStart);
                    if (valueStart > 0 && valueEnd > valueStart) {
                        String fullPath = arguments.substring(valueStart, valueEnd);
                        // 只返回文件名部分
                        int lastSlash = fullPath.lastIndexOf('/');
                        return lastSlash != -1 ? fullPath.substring(lastSlash + 1) : fullPath;
                    }
                }
                // 查找fileName字段
                if (arguments.contains("\"fileName\"")) {
                    int start = arguments.indexOf("\"fileName\"") + 12; // "fileName": 的长度
                    int valueStart = arguments.indexOf("\"", start) + 1;
                    int valueEnd = arguments.indexOf("\"", valueStart);
                    if (valueStart > 0 && valueEnd > valueStart) {
                        return arguments.substring(valueStart, valueEnd);
                    }
                }
                // 查找path字段
                if (arguments.contains("\"path\"")) {
                    int start = arguments.indexOf("\"path\"") + 8; // "path": 的长度
                    int valueStart = arguments.indexOf("\"", start) + 1;
                    int valueEnd = arguments.indexOf("\"", valueStart);
                    if (valueStart > 0 && valueEnd > valueStart) {
                        String fullPath = arguments.substring(valueStart, valueEnd);
                        // 只返回文件名部分
                        int lastSlash = fullPath.lastIndexOf('/');
                        return lastSlash != -1 ? fullPath.substring(lastSlash + 1) : fullPath;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract file name from arguments: {}", e.getMessage());
        }
        return "unknown file";
    }



}
