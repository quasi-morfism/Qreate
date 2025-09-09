package com.morfism.aiappgenerator.core.parser;

import com.morfism.aiappgenerator.ai.model.HtmlCodeResult;
import com.morfism.aiappgenerator.ai.model.MultiFileCodeResult;
import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.exception.ErrorCode;
import com.morfism.aiappgenerator.model.enums.CodeGenTypeEnum;

public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();
    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    /**
     * Execute code parser based on generation type
     *
     * @param codeContent the code content to parse
     * @param type        the code generation type
     * @return parsed result object (HtmlCodeResult or MultiFileCodeResult)
     * @throws BusinessException if parameters are invalid or type is unsupported
     */
    public static Object executeParser(String codeContent, CodeGenTypeEnum type) {
        if (codeContent == null || codeContent.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Code content cannot be empty");
        }
        if (type == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Code generation type cannot be null");
        }
        
        return switch (type) {
            case HTML -> htmlCodeParser.parseCode(codeContent);
            case MULTI_FILE -> multiFileCodeParser.parseCode(codeContent);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "Unsupported code generation type: " + type.getValue());
        };
    }

}
