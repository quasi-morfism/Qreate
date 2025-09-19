package com.morfism.aiappgenerator.ai;

import com.morfism.aiappgenerator.ai.model.HtmlCodeResult;
import com.morfism.aiappgenerator.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;

public interface AiCodeGeneratorService {

    /**
     * 生成html代码
     * @param userMessage 用户 prompt
     * @return ai response
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成代码
     * @param userMessage 用户 prompt
     * @return ai response
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 生成 HTML 代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 生成多文件代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);

    /**
     * 生成应用名称
     * @param initPrompt 初始化提示词
     * @return 生成的应用名称
     */
    @SystemMessage(fromResource = "prompt/app-name-generation-system-prompt.txt")
    String generateAppName(String initPrompt);

    /**
     * 生成 Vue 项目代码（流式）
     *
     * @param userMessage 用户消息
     * @return 生成过程的流式响应
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    Flux<String> generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);

    /**
     * 生成 Vue 项目代码（TokenStream，支持工具执行跟踪）
     *
     * @param appId 应用ID
     * @param userMessage 用户消息
     * @return TokenStream，支持工具执行回调
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectTokenStream(@MemoryId long appId, @UserMessage String userMessage);

}
