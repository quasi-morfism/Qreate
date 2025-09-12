package com.morfism.aiappgenerator.core;

import com.morfism.aiappgenerator.ai.AiCodeGeneratorService;
import com.morfism.aiappgenerator.ai.model.HtmlCodeResult;
import com.morfism.aiappgenerator.ai.model.MultiFileCodeResult;

import com.morfism.aiappgenerator.core.parser.CodeParserExecutor;
import com.morfism.aiappgenerator.core.saver.CodeFileSaverExecutor;
import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.exception.ErrorCode;
import com.morfism.aiappgenerator.model.enums.CodeGenTypeEnum;
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
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一代码生成入口：根据类型生成并保存代码（同步模式）
     * Unified entry point: generate and save code based on type
     *
     * @param userMessage     用户提示词/user prompt
     * @param codeGenTypeEnum 代码生成类型/generation type
     * @param appId           应用ID/application ID
     * @return 保存的目录/saved directory
     * @throws BusinessException 当生成类型为null或不支持时抛出异常
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Generation type is null");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "Unsupported generation type: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }



    /**
     * 处理代码流并在完成时保存到文件
     * Process code stream and save to files when complete
     *
     * @param codestream 代码内容的响应式流/the reactive stream of code content
     * @param codeType   代码生成类型/the type of code generation
     * @param appId      应用ID/application ID
     * @return 用于进一步处理的原始流/the original stream for further processing
     */
    private Flux<String> CodeStream(Flux<String> codestream, CodeGenTypeEnum codeType, Long appId) {
        // Collect code chunks during streaming and save when complete
        StringBuilder codeBuilder = new StringBuilder();
        return codestream
                .doOnNext(chunk -> {
                    // Collect code chunks in real-time
                    codeBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // Save code after streaming is complete
                    try {
                        String completeCode = codeBuilder.toString();
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeType);
                        // Save parsed code to files
                        File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeType, appId);
                        log.info("Code saved successfully to: " + savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("Failed to save code: {}", e.getMessage());
                    }
                });
    }

    /**
     * 统一流式代码生成入口：根据类型生成并保存代码（异步流式模式）
     * Unified streaming entry point: generate and save code based on type
     * Provides reactive streaming of code generation with automatic file saving
     *
     * @param userMessage     用户代码生成提示词/user prompt for code generation
     * @param codeGenTypeEnum 要生成的代码类型(HTML或MULTI_FILE)/type of code to generate (HTML or MULTI_FILE)
     * @param appId           应用ID/application ID
     * @return 生成的代码块响应式流/reactive stream of generated code chunks
     * @throws BusinessException 当codeGenTypeEnum为null或不支持时抛出异常/if codeGenTypeEnum is null or unsupported
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Code generation type is null");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield CodeStream(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield CodeStream(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "Unsupported code generation type: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }


}
