package com.morfism.aiappgenerator.core.saver;

import cn.hutool.core.util.StrUtil;
import com.morfism.aiappgenerator.ai.model.HtmlCodeResult;
import com.morfism.aiappgenerator.exception.BusinessException;
import com.morfism.aiappgenerator.exception.ErrorCode;
import com.morfism.aiappgenerator.model.enums.CodeGenTypeEnum;

import javax.swing.text.html.HTML;

public class HtmlFileCodeSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult>{


    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Html code cannot be empty");
        }
    }
}
