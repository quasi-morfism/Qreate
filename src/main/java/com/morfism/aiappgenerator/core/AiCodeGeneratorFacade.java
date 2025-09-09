package com.morfism.aiappgenerator.core;

import com.morfism.aiappgenerator.ai.AiCodeGeneratorService;
import com.morfism.aiappgenerator.ai.model.HtmlCodeResult;
import com.morfism.aiappgenerator.ai.model.MultiFileCodeResult;
import com.morfism.aiappgenerator.core.parser.CodeParser;
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
 * AI code generation facade class, combining generation and saving functionality
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * Unified entry point: generate and save code based on type
     *
     * @param userMessage     user prompt
     * @param codeGenTypeEnum generation type
     * @return saved directory
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Generation type is null");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE);
            }
            default -> {
                String errorMessage = "Unsupported generation type: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }



    /**
     * Process code stream and save to files when complete
     *
     * @param codestream the reactive stream of code content
     * @param codeType the type of code generation
     * @return the original stream for further processing
     */
    private Flux<String> CodeStream(Flux<String> codestream, CodeGenTypeEnum codeType) {
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
                        File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeType);
                        log.info("Code saved successfully to: " + savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("Failed to save code: {}", e.getMessage());
                    }
                });
    }

    /**
     * Unified streaming entry point: generate and save code based on type
     * Provides reactive streaming of code generation with automatic file saving
     *
     * @param userMessage     user prompt for code generation
     * @param codeGenTypeEnum type of code to generate (HTML or MULTI_FILE)
     * @return reactive stream of generated code chunks
     * @throws BusinessException if codeGenTypeEnum is null or unsupported
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Code generation type is null");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield CodeStream(result, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield CodeStream(result, CodeGenTypeEnum.MULTI_FILE);
            }
            default -> {
                String errorMessage = "Unsupported code generation type: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }


}
